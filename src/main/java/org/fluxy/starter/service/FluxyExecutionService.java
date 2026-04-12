package org.fluxy.starter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.model.StepStatus;
import org.fluxy.core.model.TaskResult;
import org.fluxy.core.model.TaskStatus;
import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.spring.persistence.entity.FlowStepEntity;
import org.fluxy.spring.persistence.entity.FluxyFlowEntity;
import org.fluxy.spring.persistence.entity.FluxyStepEntity;
import org.fluxy.spring.persistence.entity.StepTaskEntity;
import org.fluxy.spring.persistence.repository.FlowStepRepository;
import org.fluxy.spring.persistence.repository.FluxyFlowRepository;
import org.fluxy.spring.persistence.repository.FluxyStepRepository;
import org.fluxy.spring.persistence.repository.StepTaskRepository;
import org.fluxy.starter.dto.ExecutionContextRequest;
import org.fluxy.starter.dto.FlowExecutionResultDto;
import org.fluxy.starter.dto.FlowStepExecutionDto;
import org.fluxy.starter.dto.StepExecutionResultDto;
import org.fluxy.starter.dto.TaskExecutionDto;
import org.fluxy.starter.dto.TaskExecutionResultDto;
import org.fluxy.starter.registration.FluxyTaskRegistry;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de ejecución de Fluxy — orquesta la inicialización de flows,
 * el procesamiento de steps y la ejecución de tasks a demanda.
 *
 * <p>Actúa como puente entre las entidades JPA de {@code fluxy-spring} y
 * los servicios del motor de ejecución de {@code fluxy-core}, resolviendo
 * las instancias vivas de {@link FluxyTask} desde el {@link FluxyTaskRegistry}
 * y delegando la ejecución real al {@link TaskExecutorService} que publica
 * eventos vía el {@link org.fluxy.core.service.FluxyEventsBus} configurado.</p>
 *
 * <p>El estado de ejecución (statuses de steps y tasks) se persiste en las
 * entidades JPA, permitiendo un modelo de ejecución paso a paso (step-by-step)
 * controlado por el cliente.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FluxyExecutionService {

    private final FluxyFlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final FluxyStepRepository stepRepository;
    private final StepTaskRepository stepTaskRepository;
    private final FluxyTaskRegistry taskRegistry;
    private final TaskExecutorService taskExecutorService;

    // ── Flow execution ──────────────────────────────────────────────────────

    /**
     * Inicializa la ejecución de un flow: establece todos los steps y tasks
     * en estado {@code PENDING}, limpiando cualquier resultado previo.
     *
     * @param flowId  identificador del flow a inicializar
     * @param request contexto de ejecución (no se usa para la inicialización,
     *                pero se valida que el flow exista)
     * @return estado completo del flow tras la inicialización
     * @throws IllegalArgumentException si el flow no existe
     */
    public FlowExecutionResultDto initializeFlow(UUID flowId, ExecutionContextRequest request) {
        FluxyFlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowId));

        List<FlowStepEntity> steps = flowStepRepository.findByFlowOrderByStepOrderAsc(flowEntity);
        for (FlowStepEntity flowStep : steps) {
            flowStep.setStepStatus(StepStatus.PENDING);
            List<StepTaskEntity> tasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(flowStep.getStep());
            for (StepTaskEntity task : tasks) {
                task.setStatus(TaskStatus.PENDING);
                task.setResult(null);
            }
            stepTaskRepository.saveAll(tasks);
        }
        flowStepRepository.saveAll(steps);

        log.info("[Fluxy] Flow '{}' (id={}) inicializado. {} step(s) en PENDING.",
                flowEntity.getName(), flowId, steps.size());

        return toFlowResult(flowEntity, steps);
    }

    /**
     * Procesa el siguiente paso en un flow: encuentra el step actualmente
     * en ejecución (RUNNING) o el siguiente pendiente (PENDING), y ejecuta
     * su siguiente tarea pendiente.
     *
     * <p>Cuando todas las tareas de un step se completan, el step pasa a
     * {@code FINISHED} automáticamente.</p>
     *
     * @param flowId  identificador del flow a procesar
     * @param request contexto de ejecución con variables y referencias
     * @return estado completo del flow tras el procesamiento
     * @throws IllegalArgumentException si el flow no existe
     * @throws IllegalStateException    si no quedan steps pendientes
     */
    public FlowExecutionResultDto processFlow(UUID flowId, ExecutionContextRequest request) {
        FluxyFlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowId));

        ExecutionContext ctx = toExecutionContext(request);

        List<FlowStepEntity> steps = flowStepRepository.findByFlowOrderByStepOrderAsc(flowEntity);

        // Buscar step actualmente en RUNNING, o el siguiente PENDING
        FlowStepEntity currentStep = steps.stream()
                .filter(s -> s.getStepStatus() == StepStatus.RUNNING)
                .findFirst()
                .orElseGet(() -> steps.stream()
                        .filter(s -> s.getStepStatus() == StepStatus.PENDING)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No hay steps pendientes en el flow: " + flowId)));

        if (currentStep.getStepStatus() == StepStatus.PENDING) {
            currentStep.setStepStatus(StepStatus.RUNNING);
            flowStepRepository.save(currentStep);
        }

        // Ejecutar siguiente tarea del step
        processStepInternal(currentStep.getStep(), ctx);

        // Verificar si todas las tareas del step terminaron
        List<StepTaskEntity> stepTasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(currentStep.getStep());
        boolean allDone = stepTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.FINISHED);
        if (allDone) {
            currentStep.setStepStatus(StepStatus.FINISHED);
            flowStepRepository.save(currentStep);
            log.info("[Fluxy] Step '{}' completado en flow '{}'.",
                    currentStep.getStep().getName(), flowEntity.getName());
        }

        // Refrescar steps para el resultado
        List<FlowStepEntity> refreshedSteps = flowStepRepository.findByFlowOrderByStepOrderAsc(flowEntity);
        return toFlowResult(flowEntity, refreshedSteps);
    }

    // ── Step execution ──────────────────────────────────────────────────────

    /**
     * Procesa un step a demanda: encuentra la siguiente tarea pendiente
     * dentro del step y la ejecuta.
     *
     * @param stepId  identificador del step a procesar
     * @param request contexto de ejecución con variables y referencias
     * @return estado del step tras el procesamiento
     * @throws IllegalArgumentException si el step no existe
     * @throws IllegalStateException    si no quedan tareas pendientes en el step
     */
    public StepExecutionResultDto processStep(UUID stepId, ExecutionContextRequest request) {
        FluxyStepEntity stepEntity = stepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepId));

        ExecutionContext ctx = toExecutionContext(request);
        processStepInternal(stepEntity, ctx);

        List<StepTaskEntity> refreshedTasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(stepEntity);
        return toStepResult(stepEntity, refreshedTasks);
    }

    /**
     * Inicializa un flow por nombre.
     * Resuelve el nombre a ID y delega a {@link #initializeFlow(UUID, ExecutionContextRequest)}.
     */
    public FlowExecutionResultDto initializeFlowByName(String flowName, ExecutionContextRequest request) {
        FluxyFlowEntity flowEntity = flowRepository.findByName(flowName)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowName));
        return initializeFlow(flowEntity.getId(), request);
    }

    /**
     * Procesa un flow por nombre.
     * Resuelve el nombre a ID y delega a {@link #processFlow(UUID, ExecutionContextRequest)}.
     */
    public FlowExecutionResultDto processFlowByName(String flowName, ExecutionContextRequest request) {
        FluxyFlowEntity flowEntity = flowRepository.findByName(flowName)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowName));
        return processFlow(flowEntity.getId(), request);
    }

    /**
     * Procesa un step por nombre.
     * Resuelve el nombre a ID y delega a {@link #processStep(UUID, ExecutionContextRequest)}.
     */
    public StepExecutionResultDto processStepByName(String stepName, ExecutionContextRequest request) {
        FluxyStepEntity stepEntity = stepRepository.findByName(stepName)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepName));
        return processStep(stepEntity.getId(), request);
    }

    // ── Task execution ──────────────────────────────────────────────────────

    /**
     * Ejecuta una tarea específica a demanda, buscándola por nombre en el
     * {@link FluxyTaskRegistry}. Si existen varias versiones registradas con
     * el mismo nombre, se utiliza la de <b>mayor versión</b>.
     *
     * <p>La ejecución se delega al {@link TaskExecutorService} del core, que
     * publica un evento {@link org.fluxy.core.model.FluxyEvent} en el bus
     * configurado (SPRING, SQS o RABBIT).</p>
     *
     * @param taskName nombre de la tarea a ejecutar
     * @param request  contexto de ejecución con variables y referencias
     * @return resultado de la ejecución
     * @throws IllegalArgumentException si la tarea no existe en el registro
     */
    public TaskExecutionResultDto executeTask(String taskName, ExecutionContextRequest request) {
        FluxyTask fluxyTask = taskRegistry.findLatestByName(taskName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea no encontrada en el registro: " + taskName));

        ExecutionContext ctx = toExecutionContext(request);

        log.info("[Fluxy] Ejecutando tarea '{}' (v{}) a demanda.", taskName, fluxyTask.getVersion());
        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        return new TaskExecutionResultDto(taskName, TaskStatus.FINISHED.name(), result.name());
    }

    /**
     * Ejecuta una tarea específica a demanda, buscándola por nombre <b>y
     * versión exacta</b> en el {@link FluxyTaskRegistry}.
     *
     * @param taskName nombre de la tarea a ejecutar
     * @param version  versión exacta requerida
     * @param request  contexto de ejecución con variables y referencias
     * @return resultado de la ejecución
     * @throws IllegalArgumentException si no existe una tarea con ese nombre y versión
     */
    public TaskExecutionResultDto executeTask(String taskName, int version, ExecutionContextRequest request) {
        FluxyTask fluxyTask = taskRegistry.findByNameAndVersion(taskName, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea no encontrada en el registro: '%s' v%d".formatted(taskName, version)));

        ExecutionContext ctx = toExecutionContext(request);

        log.info("[Fluxy] Ejecutando tarea '{}' (v{}) a demanda.", taskName, version);
        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        return new TaskExecutionResultDto(taskName, TaskStatus.FINISHED.name(), result.name());
    }

    // ── Lógica interna ──────────────────────────────────────────────────────

    /**
     * Busca la siguiente tarea pendiente o en ejecución del step, resuelve
     * la instancia viva del {@link FluxyTask} desde el registro y la ejecuta
     * vía {@link TaskExecutorService}. Actualiza el estado en BD.
     */
    private void processStepInternal(FluxyStepEntity stepEntity, ExecutionContext ctx) {
        List<StepTaskEntity> tasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(stepEntity);

        StepTaskEntity currentTask = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.RUNNING)
                .findFirst()
                .orElseGet(() -> tasks.stream()
                        .filter(t -> t.getStatus() == TaskStatus.PENDING)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No hay tareas pendientes en el step: " + stepEntity.getName())));

        String taskName = currentTask.getTask().getName();
        int taskVersion = currentTask.getTask().getVersion();
        FluxyTask fluxyTask = taskRegistry.findByNameAndVersion(taskName, taskVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea '%s' v%d no encontrada en el registro. ".formatted(taskName, taskVersion) +
                        "Asegúrese de que el bean @Task esté en el contexto de Spring con la versión correcta."));

        currentTask.setStatus(TaskStatus.RUNNING);
        stepTaskRepository.save(currentTask);

        log.debug("[Fluxy] Ejecutando tarea '{}' (orden={}) del step '{}'.",
                taskName, currentTask.getTaskOrder(), stepEntity.getName());

        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        currentTask.setResult(result);
        currentTask.setStatus(TaskStatus.FINISHED);
        stepTaskRepository.save(currentTask);

        log.debug("[Fluxy] Tarea '{}' finalizada con resultado: {}", taskName, result);
    }

    // ── Conversiones ────────────────────────────────────────────────────────

    private ExecutionContext toExecutionContext(ExecutionContextRequest request) {
        ExecutionContext ctx = new ExecutionContext(
                request != null && request.type() != null ? request.type() : "default",
                request != null && request.version() != null ? request.version() : "1.0"
        );
        if (request != null && request.variables() != null) {
            request.variables().forEach(v -> ctx.addParameter(v.name(), v.value()));
        }
        if (request != null && request.references() != null) {
            request.references().forEach(r -> ctx.addReference(r.type(), r.value()));
        }
        return ctx;
    }

    private FlowExecutionResultDto toFlowResult(FluxyFlowEntity entity, List<FlowStepEntity> steps) {
        List<FlowStepExecutionDto> stepDtos = steps.stream()
                .map(fs -> {
                    List<StepTaskEntity> tasks = stepTaskRepository
                            .findByStepOrderByTaskOrderAsc(fs.getStep());
                    List<TaskExecutionDto> taskDtos = tasks.stream()
                            .map(this::toTaskExecutionDto)
                            .toList();
                    return new FlowStepExecutionDto(
                            fs.getStep().getId(),
                            fs.getStep().getName(),
                            fs.getStepOrder(),
                            fs.getStepStatus() != null ? fs.getStepStatus().name() : null,
                            taskDtos
                    );
                })
                .toList();
        return new FlowExecutionResultDto(
                entity.getId(), entity.getName(), entity.getType(), stepDtos);
    }

    private StepExecutionResultDto toStepResult(FluxyStepEntity entity, List<StepTaskEntity> tasks) {
        List<TaskExecutionDto> taskDtos = tasks.stream()
                .map(this::toTaskExecutionDto)
                .toList();
        return new StepExecutionResultDto(entity.getId(), entity.getName(), taskDtos);
    }

    private TaskExecutionDto toTaskExecutionDto(StepTaskEntity st) {
        return new TaskExecutionDto(
                st.getTask().getName(),
                st.getTaskOrder(),
                st.getStatus() != null ? st.getStatus().name() : null,
                st.getResult() != null ? st.getResult().name() : null
        );
    }
}

