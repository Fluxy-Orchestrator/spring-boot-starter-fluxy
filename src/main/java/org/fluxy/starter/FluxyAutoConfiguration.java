package org.fluxy.starter;

import org.fluxy.core.model.FluxyTask;
import org.fluxy.starter.autoconfigure.FluxyAwsSqsEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyDataAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyDedicatedDataSourceAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyExecutionEngineAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyRabbitMqEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxySpringEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyWebAutoConfiguration;
import org.fluxy.starter.properties.FluxyDataSourceProperties;
import org.fluxy.starter.properties.FluxyEventBusProperties;
import org.fluxy.starter.properties.FluxyJpaProperties;
import org.fluxy.starter.properties.FluxyTaskRegistrationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuración principal del starter {@code spring-boot-starter-fluxy}.
 *
 * <p>Importa automáticamente:</p>
 * <ul>
 *   <li>{@link FluxyDedicatedDataSourceAutoConfiguration} — si {@code fluxy.datasource.url}
 *       está presente, crea un DataSource, EntityManagerFactory y TransactionManager dedicados.</li>
 *   <li>{@link FluxyDataAutoConfiguration} — si <b>no</b> hay datasource dedicado, utiliza el
 *       datasource primario de la aplicación para la persistencia de Fluxy.</li>
 *   <li>{@link FluxyWebAutoConfiguration} — registra controladores REST para FluxyTask,
 *       FluxyStep y FluxyFlow (condicionalmente, solo si Spring MVC está presente).</li>
 *   <li>{@link FluxySpringEventBusAutoConfiguration} — bus de eventos basado en Spring
 *       Application Events (por defecto, sin dependencias extra).</li>
 *   <li>{@link FluxyAwsSqsEventBusAutoConfiguration} — bus de eventos basado en Amazon SQS
 *       (requiere {@code fluxy.eventbus.type = SQS} y {@code spring-cloud-aws-sqs} en el classpath).</li>
 *   <li>{@link FluxyRabbitMqEventBusAutoConfiguration} — bus de eventos basado en RabbitMQ
 *       (requiere {@code fluxy.eventbus.type = RABBIT} y {@code spring-rabbit} en el classpath).</li>
 * </ul>
 *
 * <p>Además registra:</p>
 * <ul>
 *   <li>{@link FluxyTaskRegistry} — mapa en memoria de todos los beans {@link FluxyTask}
 *       disponibles en el contexto para recuperación por nombre.</li>
 *   <li>{@link TaskAutoRegistrationProcessor} — listener que al arrancar la aplicación
 *       detecta todos los beans anotados con {@code @Task} que extiendan {@link FluxyTask}
 *       y ejecuta auto-registro, cleanup de tareas obsoletas y validación de versiones en
 *       steps según la configuración en {@code fluxy.task.registration.*}.</li>
 * </ul>
 *
 * <p>Ejemplo de configuración en {@code application.yml}:</p>
 * <pre>{@code
 * fluxy:
 *   datasource:
 *     url: jdbc:postgresql://localhost:5432/fluxydb
 *     driver-class-name: org.postgresql.Driver
 *     username: fluxy_user
 *     password: fluxy_pass
 *     hikari:
 *       maximum-pool-size: 5
 *   jpa:
 *     hibernate:
 *       ddl-auto: validate
 *   task:
 *     registration:
 *       auto-register: true
 *       cleanup-stale:
 *         enabled: false
 *         mode: WARN
 *       validate-steps: false
 *   eventbus:
 *     type: SPRING          # SPRING (default) | SQS | RABBIT
 *     sqs:                  # solo cuando type = SQS
 *       queue-url: https://sqs.us-east-1.amazonaws.com/123/fluxy-events
 *       region: us-east-1
 *     rabbit:               # solo cuando type = RABBIT
 *       host: localhost
 *       port: 5672
 *       username: guest
 *       password: guest
 *       queue: fluxy-events
 *       exchange: fluxy-exchange
 *       routing-key: fluxy.events
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnClass(FluxyTask.class)
@EnableConfigurationProperties({
        FluxyDataSourceProperties.class,
        FluxyJpaProperties.class,
        FluxyTaskRegistrationProperties.class,
        FluxyEventBusProperties.class
})
@Import({
        // 1. Datasource y repositorios JPA
        FluxyDedicatedDataSourceAutoConfiguration.class,
        FluxyDataAutoConfiguration.class,
        // 2. Bus de eventos (provee FluxyEventsBus)
        FluxySpringEventBusAutoConfiguration.class,
        FluxyAwsSqsEventBusAutoConfiguration.class,
        FluxyRabbitMqEventBusAutoConfiguration.class,
        // 3. Motor de ejecución (necesita FluxyEventsBus → provee TaskExecutorService + FluxyTaskRegistry)
        FluxyExecutionEngineAutoConfiguration.class,
        // 4. Controladores REST (necesita TaskExecutorService + FluxyTaskRegistry + repos)
        FluxyWebAutoConfiguration.class
})
public class FluxyAutoConfiguration {
}
