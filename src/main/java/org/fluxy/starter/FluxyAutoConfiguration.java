package org.fluxy.starter;

import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.service.FlowExecutor;
import org.fluxy.core.service.FluxyEventsBus;
import org.fluxy.core.service.StepExecutionService;
import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.spring.persistence.repository.FluxyTaskRepository;
import org.fluxy.spring.persistence.repository.StepTaskRepository;
import org.fluxy.starter.autoconfigure.FluxyAwsSqsEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyDataAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyDedicatedDataSourceAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyRabbitMqEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxySpringEventBusAutoConfiguration;
import org.fluxy.starter.autoconfigure.FluxyWebAutoConfiguration;
import org.fluxy.starter.properties.FluxyDataSourceProperties;
import org.fluxy.starter.properties.FluxyEventBusProperties;
import org.fluxy.starter.properties.FluxyJpaProperties;
import org.fluxy.starter.properties.FluxyTaskRegistrationProperties;
import org.fluxy.starter.registration.FluxyTaskRegistry;
import org.fluxy.starter.registration.TaskAutoRegistrationProcessor;
import org.fluxy.starter.service.FluxyTaskService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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
        FluxyDedicatedDataSourceAutoConfiguration.class,
        FluxyDataAutoConfiguration.class,
        FluxyWebAutoConfiguration.class,
        FluxySpringEventBusAutoConfiguration.class,
        FluxyAwsSqsEventBusAutoConfiguration.class,
        FluxyRabbitMqEventBusAutoConfiguration.class
})
public class FluxyAutoConfiguration {

    /**
     * Registro en memoria de todos los beans {@link FluxyTask} del contexto.
     * Permite buscar instancias por nombre para usarlas en la ejecución de flows.
     */
    @Bean
    public FluxyTaskRegistry fluxyTaskRegistry(ApplicationContext applicationContext) {
        return new FluxyTaskRegistry(applicationContext);
    }

    // ── Servicios del motor de ejecución (fluxy-core) ────────────────────────

    /**
     * Servicio de ejecución de tareas del core.
     * Ejecuta una {@link FluxyTask} dentro de un {@link org.fluxy.core.model.ExecutionContext}
     * y publica el resultado como {@link org.fluxy.core.model.FluxyEvent} en el bus configurado.
     *
     * <p>El {@link FluxyEventsBus} se resuelve automáticamente según la propiedad
     * {@code fluxy.eventbus.type}: SPRING (default), SQS o RABBIT.</p>
     */
    @Bean
    @ConditionalOnBean(FluxyEventsBus.class)
    public TaskExecutorService taskExecutorService(FluxyEventsBus eventsBus) {
        return new TaskExecutorService(eventsBus);
    }

    /**
     * Servicio de ejecución de steps del core.
     * Orquesta la ejecución secuencial de tareas dentro de un step.
     */
    @Bean
    @ConditionalOnBean({TaskExecutorService.class, FluxyEventsBus.class})
    public StepExecutionService stepExecutionService(
            TaskExecutorService taskExecutorService,
            FluxyEventsBus eventsBus) {
        return new StepExecutionService(taskExecutorService, eventsBus);
    }

    /**
     * Ejecutor de flows del core.
     * Orquesta la ejecución secuencial de steps dentro de un flow,
     * evaluando conexiones y condiciones entre steps.
     */
    @Bean
    @ConditionalOnBean({FluxyEventsBus.class, StepExecutionService.class})
    public FlowExecutor flowExecutor(
            FluxyEventsBus eventsBus,
            StepExecutionService stepExecutionService) {
        return new FlowExecutor(eventsBus, stepExecutionService);
    }

    /**
     * Procesador de auto-registro y validación que, tras el arranque completo
     * de la aplicación, sincroniza en la base de datos todas las implementaciones
     * de {@link FluxyTask} anotadas con {@code @Task} y ejecuta las validaciones
     * de consistencia configuradas.
     */
    @Bean
    @ConditionalOnBean({FluxyTaskRepository.class, StepTaskRepository.class})
    public TaskAutoRegistrationProcessor taskAutoRegistrationProcessor(
            ApplicationContext applicationContext,
            FluxyTaskService fluxyTaskService,
            FluxyTaskRepository fluxyTaskRepository,
            StepTaskRepository stepTaskRepository,
            FluxyTaskRegistrationProperties registrationProperties) {
        return new TaskAutoRegistrationProcessor(
                applicationContext,
                fluxyTaskService,
                fluxyTaskRepository,
                stepTaskRepository,
                registrationProperties
        );
    }
}
