package org.fluxy.starter.autoconfigure;

import org.fluxy.starter.eventbus.FluxyEventBusObjectMapperFactory;
import org.fluxy.starter.eventbus.FluxyEventHandler;
import org.fluxy.starter.eventbus.rabbit.FluxyRabbitListener;
import org.fluxy.starter.eventbus.rabbit.RabbitFluxyEventBus;
import org.fluxy.starter.properties.FluxyEventBusProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuración del bus de eventos basado en <b>RabbitMQ</b>.
 *
 * <p>Se activa cuando:</p>
 * <ul>
 *   <li>{@code fluxy.eventbus.type = RABBIT}</li>
 *   <li>{@code org.springframework.amqp:spring-rabbit} está en el classpath</li>
 * </ul>
 *
 * <p>Crea una infraestructura RabbitMQ propia para Fluxy (connection factory,
 * template, queue, exchange, binding y listener) aislada de la configuración
 * RabbitMQ principal de la aplicación, usando las propiedades definidas en
 * {@code fluxy.eventbus.rabbit.*}.</p>
 *
 * <p>Ejemplo de configuración:</p>
 * <pre>{@code
 * fluxy:
 *   eventbus:
 *     type: RABBIT
 *     rabbit:
 *       host: localhost
 *       port: 5672
 *       username: guest
 *       password: guest
 *       virtual-host: /
 *       queue: fluxy-events
 *       exchange: fluxy-exchange
 *       routing-key: fluxy.events
 * }</pre>
 */
@Configuration
@ConditionalOnProperty(prefix = "fluxy.eventbus", name = "type", havingValue = "RABBIT")
@ConditionalOnClass(RabbitTemplate.class)
public class FluxyRabbitMqEventBusAutoConfiguration {

    /**
     * {@link CachingConnectionFactory} dedicado para Fluxy, configurado con
     * los parámetros de {@code fluxy.eventbus.rabbit.*} — completamente
     * independiente del {@code ConnectionFactory} principal de la aplicación.
     */
    @Bean
    public CachingConnectionFactory fluxyRabbitConnectionFactory(FluxyEventBusProperties properties) {
        FluxyEventBusProperties.Rabbit rabbit = properties.getRabbit();
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getPort());
        factory.setUsername(rabbit.getUsername());
        factory.setPassword(rabbit.getPassword());
        factory.setVirtualHost(rabbit.getVirtualHost());
        return factory;
    }

    /** Template de Rabbit que usa el connection factory dedicado de Fluxy. */
    @Bean
    public RabbitTemplate fluxyRabbitTemplate(CachingConnectionFactory fluxyRabbitConnectionFactory) {
        return new RabbitTemplate(fluxyRabbitConnectionFactory);
    }

    /** Cola durable donde se consumen los eventos de Fluxy. */
    @Bean
    public Queue fluxyQueue(FluxyEventBusProperties properties) {
        return new Queue(properties.getRabbit().getQueue(), true);
    }

    /** Exchange de tipo topic donde se publican los eventos de Fluxy. */
    @Bean
    public TopicExchange fluxyExchange(FluxyEventBusProperties properties) {
        return new TopicExchange(properties.getRabbit().getExchange());
    }

    /** Binding entre la cola y el exchange usando el routing key configurado. */
    @Bean
    public Binding fluxyBinding(Queue fluxyQueue, TopicExchange fluxyExchange,
                                FluxyEventBusProperties properties) {
        return BindingBuilder.bind(fluxyQueue)
                .to(fluxyExchange)
                .with(properties.getRabbit().getRoutingKey());
    }

    @Bean
    public RabbitFluxyEventBus fluxyEventBus(
            RabbitTemplate fluxyRabbitTemplate,
            FluxyEventBusProperties properties,
            List<FluxyEventHandler> handlers) {
        return new RabbitFluxyEventBus(
                fluxyRabbitTemplate,
                FluxyEventBusObjectMapperFactory.create(),
                properties.getRabbit().getExchange(),
                properties.getRabbit().getRoutingKey(),
                handlers);
    }

    @Bean
    public FluxyRabbitListener fluxyRabbitListener(RabbitFluxyEventBus fluxyEventBus) {
        return new FluxyRabbitListener(fluxyEventBus, FluxyEventBusObjectMapperFactory.create());
    }
}
