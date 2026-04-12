package org.fluxy.starter.autoconfigure;

import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.spring.persistence.repository.FlowStepRepository;
import org.fluxy.spring.persistence.repository.FluxyFlowRepository;
import org.fluxy.spring.persistence.repository.FluxyStepRepository;
import org.fluxy.spring.persistence.repository.FluxyTaskRepository;
import org.fluxy.spring.persistence.repository.StepTaskRepository;
import org.fluxy.starter.registration.FluxyTaskRegistry;
import org.fluxy.starter.web.FluxyExecutionController;
import org.fluxy.starter.web.FluxyFlowController;
import org.fluxy.starter.web.FluxyStepController;
import org.fluxy.starter.web.FluxyTaskController;
import org.fluxy.starter.service.FluxyExecutionService;
import org.fluxy.starter.service.FluxyFlowService;
import org.fluxy.starter.service.FluxyStepService;
import org.fluxy.starter.service.FluxyTaskService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configura los controladores REST de Fluxy de forma condicional.
 *
 * <p>Se activa únicamente cuando:
 * <ul>
 *   <li>La aplicación es una aplicación web ({@link ConditionalOnWebApplication})</li>
 *   <li>{@link DispatcherServlet} está en el classpath (Spring MVC presente)</li>
 *   <li>Los servicios Fluxy ya han sido registrados ({@link ConditionalOnBean})</li>
 * </ul>
 * </p>
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(DispatcherServlet.class)
public class FluxyWebAutoConfiguration {

    @Bean
    @ConditionalOnBean(FluxyTaskRepository.class)
    public FluxyTaskService fluxyTaskService(FluxyTaskRepository fluxyTaskRepository) {
        return new FluxyTaskService(fluxyTaskRepository);
    }

    @Bean
    @ConditionalOnBean({FluxyStepRepository.class, StepTaskRepository.class, FluxyTaskRepository.class})
    public FluxyStepService fluxyStepService(
            FluxyStepRepository fluxyStepRepository,
            StepTaskRepository stepTaskRepository,
            FluxyTaskRepository fluxyTaskRepository) {
        return new FluxyStepService(fluxyStepRepository, stepTaskRepository, fluxyTaskRepository);
    }

    @Bean
    @ConditionalOnBean({FluxyFlowRepository.class, FlowStepRepository.class, FluxyStepRepository.class})
    public FluxyFlowService fluxyFlowService(
            FluxyFlowRepository fluxyFlowRepository,
            FlowStepRepository flowStepRepository,
            FluxyStepRepository fluxyStepRepository) {
        return new FluxyFlowService(fluxyFlowRepository, flowStepRepository, fluxyStepRepository);
    }

    @Bean
    @ConditionalOnBean(FluxyTaskService.class)
    public FluxyTaskController fluxyTaskController(FluxyTaskService fluxyTaskService) {
        return new FluxyTaskController(fluxyTaskService);
    }

    @Bean
    @ConditionalOnBean(FluxyStepService.class)
    public FluxyStepController fluxyStepController(FluxyStepService fluxyStepService) {
        return new FluxyStepController(fluxyStepService);
    }

    @Bean
    @ConditionalOnBean(FluxyFlowService.class)
    public FluxyFlowController fluxyFlowController(FluxyFlowService fluxyFlowService) {
        return new FluxyFlowController(fluxyFlowService);
    }

    // ── Ejecución de flows, steps y tasks a demanda ──────────────────────────

    @Bean
    @ConditionalOnBean({FluxyFlowRepository.class, FlowStepRepository.class,
            FluxyStepRepository.class, StepTaskRepository.class,
            TaskExecutorService.class, FluxyTaskRegistry.class})
    public FluxyExecutionService fluxyExecutionService(
            FluxyFlowRepository fluxyFlowRepository,
            FlowStepRepository flowStepRepository,
            FluxyStepRepository fluxyStepRepository,
            StepTaskRepository stepTaskRepository,
            FluxyTaskRegistry fluxyTaskRegistry,
            TaskExecutorService taskExecutorService) {
        return new FluxyExecutionService(
                fluxyFlowRepository,
                flowStepRepository,
                fluxyStepRepository,
                stepTaskRepository,
                fluxyTaskRegistry,
                taskExecutorService);
    }

    @Bean
    @ConditionalOnBean(FluxyExecutionService.class)
    public FluxyExecutionController fluxyExecutionController(
            FluxyExecutionService fluxyExecutionService) {
        return new FluxyExecutionController(fluxyExecutionService);
    }
}

