package org.fluxy.starter.eventbus.rabbit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyEvent;
import org.fluxy.starter.eventbus.FluxyEventBus;
import org.fluxy.starter.eventbus.FluxyEventHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

/**
 * Implementación de {@link FluxyEventBus} basada en RabbitMQ.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>{@code publish} serializa el {@link FluxyEvent} a JSON y lo envía al exchange.</li>
 *   <li>{@link FluxyRabbitListener} consume el mensaje con {@code @RabbitListener},
 *       lo deserializa y llama a {@code listen}.</li>
 *   <li>{@code listen} distribuye el evento a todos los {@link FluxyEventHandler}
 *       registrados en el contexto.</li>
 * </ol>
 *
 * <p>Se activa cuando {@code fluxy.eventbus.type = RABBIT} y la dependencia
 * {@code org.springframework.amqp:spring-rabbit} está en el classpath.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitFluxyEventBus implements FluxyEventBus {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;
    private final List<FluxyEventHandler> handlers;

    @Override
    public void publish(FluxyEvent<?, ?> event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.debug("Publicando FluxyEvent vía RabbitMQ: exchange={}, routingKey={}",
                    exchange, routingKey);
            rabbitTemplate.convertAndSend(exchange, routingKey, json);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException(
                    "Error serializando FluxyEvent a JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Invocado por {@link FluxyRabbitListener} cuando llega un mensaje de la cola.
     * Distribuye el evento a todos los {@link FluxyEventHandler} del contexto.
     */
    @Override
    public void listen(FluxyEvent<?, ?> event) {
        log.debug("Procesando FluxyEvent vía RabbitMQ");
        for (FluxyEventHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception ex) {
                log.error("Error en handler [{}] procesando FluxyEvent: {}",
                        handler.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }
}
