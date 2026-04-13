package org.fluxy.starter.autoconfigure;

import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.service.FlowExecutor;
import org.fluxy.core.service.FluxyEventsBus;
import org.fluxy.core.service.StepExecutionService;
import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.starter.registration.FluxyTaskRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del motor de ejecución de Fluxy.
 *
 * <p>Registra los servicios del core ({@link TaskExecutorService},
 * {@link StepExecutionService}, {@link FlowExecutor}) y el
 * {@link FluxyTaskRegistry} que actúa como mapa en memoria de los
 * beans {@link FluxyTask} del contexto.</p>
 *
 * <p>Debe procesarse <b>después</b> de las configuraciones del event bus
 * (que proporcionan {@link FluxyEventsBus}) y <b>antes</b> de
 * {@link FluxyWebAutoConfiguration} (que consume {@code TaskExecutorService}
 * y {@code FluxyTaskRegistry} para construir los controladores REST).</p>
 */
@Configuration
public class FluxyExecutionEngineAutoConfiguration {

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
}

