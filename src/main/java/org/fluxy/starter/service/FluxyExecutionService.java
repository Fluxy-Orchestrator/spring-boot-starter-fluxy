package org.fluxy.starter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.ExecutionStatus;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.model.StepStatus;
import org.fluxy.core.model.TaskResult;
import org.fluxy.core.model.TaskStatus;
import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.starter.support.ExecutionContextProxy;
import org.fluxy.spring.persistence.entity.ExecutionContextEntity;
import org.fluxy.spring.persistence.entity.ExecutionStepRecordEntity;
import org.fluxy.spring.persistence.entity.ExecutionTaskRecordEntity;
import org.fluxy.spring.persistence.entity.FlowStepEntity;
import org.fluxy.spring.persistence.entity.FluxyExecutionEntity;
import org.fluxy.spring.persistence.entity.FluxyFlowEntity;
import org.fluxy.spring.persistence.entity.FluxyStepEntity;
import org.fluxy.spring.persistence.entity.ReferenceEntity;
import org.fluxy.spring.persistence.entity.StepTaskEntity;
import org.fluxy.spring.persistence.entity.VariableEntity;
import org.fluxy.spring.persistence.repository.ExecutionContextRepository;
import org.fluxy.spring.persistence.repository.ExecutionStepRecordRepository;
import org.fluxy.spring.persistence.repository.ExecutionTaskRecordRepository;
import org.fluxy.spring.persistence.repository.FlowStepRepository;
import org.fluxy.spring.persistence.repository.FluxyExecutionRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de ejecucion de Fluxy — orquesta la inicializacion y procesamiento
 * de ejecuciones ({@link FluxyExecutionEntity}) como unidad aislada de estado.
 *
 * <p>Cada ejecucion encapsula un flow, un contexto y la traza de steps/tasks.
 * La idempotencia se garantiza mediante un hash SHA-256 de las referencias
 * iniciales del contexto: mismas referencias + mismo flow (no FINISHED)
 * retornan la ejecucion existente sin crear duplicados.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FluxyExecutionService {

    private final FluxyFlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final FluxyStepRepository stepRepository;
    private final StepTaskRepository stepTaskRepository;
    private final FluxyExecutionRepository executionRepository;
    private final ExecutionContextRepository contextRepository;
    private final ExecutionStepRecordRepository stepRecordRepository;
    private final ExecutionTaskRecordRepository taskRecordRepository;
    private final FluxyTaskRegistry taskRegistry;
    private final TaskExecutorService taskExecutorService;
    private final ObjectMapper objectMapper;

    // ── Flow initialization ──────────────────────────────────────────────────

    /**
     * Inicializa la ejecucion de un flow: valida invariantes, computa la
     * clave de idempotencia y crea (o retorna) la ejecucion correspondiente.
     *
     * <ol>
     *   <li>Valida que el request contenga referencias.</li>
     *   <li>Resuelve el flow por ID.</li>
     *   <li>Valida que {@code flow.name == request.type}.</li>
     *   <li>Computa idempotencyKey (SHA-256 de referencias ordenadas).</li>
     *   <li>Si existe una ejecucion activa con esa clave, la retorna (idempotente).</li>
     *   <li>Si no, crea ExecutionContext, FluxyExecution y step records.</li>
     * </ol>
     *
     * @param flowId  identificador del flow a ejecutar
     * @param request contexto de ejecucion obligatorio (con referencias)
     * @return estado completo de la ejecucion
     */
    public FlowExecutionResultDto initializeFlow(UUID flowId, ExecutionContextRequest request) {
        // 1. Validar referencias
        if (request == null || request.references() == null || request.references().isEmpty()) {
            throw new IllegalArgumentException(
                    "El contexto debe contener al menos una referencia para inicializar una ejecucion.");
        }

        // 2. Resolver flow
        FluxyFlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowId));

        // 3. Validar flow.name == context.type
        String contextType = request.type() != null ? request.type() : "default";
        if (!flowEntity.getName().equals(contextType)) {
            throw new IllegalArgumentException(
                    "El flow '%s' no es compatible con un contexto de tipo '%s'. El nombre del flow y el tipo del contexto deben coincidir."
                            .formatted(flowEntity.getName(), contextType));
        }

        // 4. Computar idempotency key
        String idempotencyKey = computeIdempotencyKey(request);

        // 5. Buscar ejecucion activa existente (idempotencia)
        Optional<FluxyExecutionEntity> existing = executionRepository
                .findByFlowAndIdempotencyKeyAndStatusNot(flowEntity, idempotencyKey, ExecutionStatus.FINISHED);
        if (existing.isPresent()) {
            log.info("[Fluxy] Ejecucion idempotente encontrada (id={}) para flow '{}'. Retornando existente.",
                    existing.get().getId(), flowEntity.getName());
            return toFlowResult(existing.get());
        }

        // 6. Crear contexto
        ExecutionContextEntity contextEntity = new ExecutionContextEntity();
        contextEntity.setType(contextType);
        contextEntity.setVersion(request.version() != null ? request.version() : "1.0");

        if (request.variables() != null) {
            for (var v : request.variables()) {
                VariableEntity ve = new VariableEntity();
                ve.setName(v.name());
                ve.setValue(v.value());
                ve.setExecutionContext(contextEntity);
                contextEntity.getVariables().add(ve);
            }
        }
        for (var r : request.references()) {
            // Validar unicidad de tipo
            boolean duplicate = contextEntity.getReferences().stream()
                    .anyMatch(ref -> ref.getRefType().equals(r.type()));
            if (duplicate) {
                throw new IllegalArgumentException(
                        "Referencia duplicada de tipo '%s'. Cada tipo de referencia debe ser unico."
                                .formatted(r.type()));
            }
            ReferenceEntity re = new ReferenceEntity();
            re.setRefType(r.type());
            re.setValue(r.value());
            re.setExecutionContext(contextEntity);
            contextEntity.getReferences().add(re);
        }

        // 7. Crear ejecucion
        FluxyExecutionEntity executionEntity = new FluxyExecutionEntity();
        executionEntity.setFlow(flowEntity);
        executionEntity.setContext(contextEntity);
        executionEntity.setStatus(ExecutionStatus.PENDING);
        executionEntity.setIdempotencyKey(idempotencyKey);

        // 8. Crear step records (uno por cada FlowStep del flow, todos PENDING)
        List<FlowStepEntity> flowSteps = flowStepRepository.findByFlowOrderByStepOrderAsc(flowEntity);
        for (FlowStepEntity fs : flowSteps) {
            ExecutionStepRecordEntity stepRecord = new ExecutionStepRecordEntity();
            stepRecord.setExecution(executionEntity);
            stepRecord.setFlowStep(fs);
            stepRecord.setStepStatus(StepStatus.PENDING);

            // Crear task records para cada task del step
            List<StepTaskEntity> stepTasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(fs.getStep());
            for (StepTaskEntity st : stepTasks) {
                ExecutionTaskRecordEntity taskRecord = new ExecutionTaskRecordEntity();
                taskRecord.setExecutionStepRecord(stepRecord);
                taskRecord.setStepTask(st);
                taskRecord.setStatus(TaskStatus.PENDING);
                taskRecord.setResult(null);
                stepRecord.getTaskRecords().add(taskRecord);
            }

            executionEntity.getStepRecords().add(stepRecord);
        }

        executionRepository.save(executionEntity);

        log.info("[Fluxy] Ejecucion creada (id={}) para flow '{}' con {} step(s).",
                executionEntity.getId(), flowEntity.getName(), flowSteps.size());

        return toFlowResult(executionEntity);
    }

    /**
     * Inicializa un flow por nombre.
     */
    public FlowExecutionResultDto initializeFlowByName(String flowName, ExecutionContextRequest request) {
        FluxyFlowEntity flowEntity = flowRepository.findByName(flowName)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowName));
        return initializeFlow(flowEntity.getId(), request);
    }

    // ── Flow processing ──────────────────────────────────────────────────────

    /**
     * Procesa el siguiente paso de una ejecucion: busca el step en RUNNING
     * o el siguiente PENDING, ejecuta su siguiente tarea pendiente y
     * actualiza el estado.
     *
     * @param executionId identificador de la ejecucion
     * @param request     contexto adicional (variables opcionales para inyectar)
     * @return estado completo de la ejecucion tras el procesamiento
     */
    public FlowExecutionResultDto processExecution(UUID executionId, ExecutionContextRequest request) {
        FluxyExecutionEntity executionEntity = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Ejecucion no encontrada: " + executionId));

        ExecutionContext ctx = toExecutionContext(executionEntity, request);

        List<ExecutionStepRecordEntity> stepRecords =
                stepRecordRepository.findByExecutionOrderByFlowStepStepOrderAsc(executionEntity);

        // Buscar step RUNNING o siguiente PENDING
        ExecutionStepRecordEntity currentStepRecord = stepRecords.stream()
                .filter(sr -> sr.getStepStatus() == StepStatus.RUNNING)
                .findFirst()
                .orElseGet(() -> stepRecords.stream()
                        .filter(sr -> sr.getStepStatus() == StepStatus.PENDING)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No hay steps pendientes en la ejecucion: " + executionId)));

        if (currentStepRecord.getStepStatus() == StepStatus.PENDING) {
            currentStepRecord.setStepStatus(StepStatus.RUNNING);
            stepRecordRepository.save(currentStepRecord);
        }

        // Ejecutar la siguiente tarea del step
        processStepRecord(currentStepRecord, ctx);

        // Verificar si todas las tareas del step terminaron
        List<ExecutionTaskRecordEntity> taskRecords = taskRecordRepository
                .findByExecutionStepRecord(currentStepRecord);
        boolean allDone = taskRecords.stream()
                .allMatch(tr -> tr.getStatus() == TaskStatus.FINISHED);
        if (allDone) {
            currentStepRecord.setStepStatus(StepStatus.FINISHED);
            stepRecordRepository.save(currentStepRecord);
            log.info("[Fluxy] Step '{}' completado en ejecucion '{}'.",
                    currentStepRecord.getFlowStep().getStep().getName(), executionId);
        }

        // Actualizar status de ejecucion si era PENDING
        if (executionEntity.getStatus() == ExecutionStatus.PENDING) {
            executionEntity.setStatus(ExecutionStatus.RUNNING);
            executionRepository.save(executionEntity);
        }

        return toFlowResult(executionEntity);
    }


    // ── Step execution (standalone) ──────────────────────────────────────────

    /**
     * Procesa un step a demanda (fuera del contexto de una ejecucion).
     */
    public StepExecutionResultDto processStep(UUID stepId, ExecutionContextRequest request) {
        FluxyStepEntity stepEntity = stepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepId));

        ExecutionContext ctx = toSimpleExecutionContext(request);
        List<StepTaskEntity> tasks = stepTaskRepository.findByStepOrderByTaskOrderAsc(stepEntity);

        StepTaskEntity currentTask = tasks.stream()
                .filter(t -> true) // standalone — ejecuta la primera disponible
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay tareas en el step: " + stepEntity.getName()));

        String taskName = currentTask.getTask().getName();
        int taskVersion = currentTask.getTask().getVersion();
        FluxyTask fluxyTask = taskRegistry.findByNameAndVersion(taskName, taskVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea '%s' v%d no encontrada en el registro.".formatted(taskName, taskVersion)));

        taskExecutorService.executeTask(fluxyTask, ctx);

        List<TaskExecutionDto> taskDtos = tasks.stream()
                .map(st -> new TaskExecutionDto(st.getTask().getName(), st.getTaskOrder(), null, null))
                .toList();
        return new StepExecutionResultDto(stepEntity.getId(), stepEntity.getName(), taskDtos);
    }

    public StepExecutionResultDto processStepByName(String stepName, ExecutionContextRequest request) {
        FluxyStepEntity stepEntity = stepRepository.findByName(stepName)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepName));
        return processStep(stepEntity.getId(), request);
    }

    // ── Task execution (standalone) ──────────────────────────────────────────

    public TaskExecutionResultDto executeTask(String taskName, ExecutionContextRequest request) {
        FluxyTask fluxyTask = taskRegistry.findLatestByName(taskName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea no encontrada en el registro: " + taskName));

        ExecutionContext ctx = toSimpleExecutionContext(request);
        log.info("[Fluxy] Ejecutando tarea '{}' (v{}) a demanda.", taskName, fluxyTask.getVersion());
        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        return new TaskExecutionResultDto(taskName, TaskStatus.FINISHED.name(), result.name());
    }

    public TaskExecutionResultDto executeTask(String taskName, int version, ExecutionContextRequest request) {
        FluxyTask fluxyTask = taskRegistry.findByNameAndVersion(taskName, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea no encontrada en el registro: '%s' v%d".formatted(taskName, version)));

        ExecutionContext ctx = toSimpleExecutionContext(request);
        log.info("[Fluxy] Ejecutando tarea '{}' (v{}) a demanda.", taskName, version);
        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        return new TaskExecutionResultDto(taskName, TaskStatus.FINISHED.name(), result.name());
    }

    // ── Query ────────────────────────────────────────────────────────────────

    /**
     * Consulta el estado actual de una ejecucion.
     */
    public FlowExecutionResultDto getExecution(UUID executionId) {
        FluxyExecutionEntity entity = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Ejecucion no encontrada: " + executionId));
        return toFlowResult(entity);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void processStepRecord(ExecutionStepRecordEntity stepRecord, ExecutionContext ctx) {
        List<ExecutionTaskRecordEntity> taskRecords = taskRecordRepository
                .findByExecutionStepRecord(stepRecord);

        ExecutionTaskRecordEntity currentTask = taskRecords.stream()
                .sorted(Comparator.comparing(tr -> tr.getStepTask().getTaskOrder()))
                .filter(tr -> tr.getStatus() == TaskStatus.RUNNING)
                .findFirst()
                .orElseGet(() -> taskRecords.stream()
                        .sorted(Comparator.comparing(tr -> tr.getStepTask().getTaskOrder()))
                        .filter(tr -> tr.getStatus() == TaskStatus.PENDING)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No hay tareas pendientes en el step: " +
                                stepRecord.getFlowStep().getStep().getName())));

        String taskName = currentTask.getStepTask().getTask().getName();
        int taskVersion = currentTask.getStepTask().getTask().getVersion();
        FluxyTask fluxyTask = taskRegistry.findByNameAndVersion(taskName, taskVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarea '%s' v%d no encontrada en el registro. ".formatted(taskName, taskVersion) +
                        "Asegurese de que el bean @Task este en el contexto de Spring con la version correcta."));

        currentTask.setStatus(TaskStatus.RUNNING);
        taskRecordRepository.save(currentTask);

        log.debug("[Fluxy] Ejecutando tarea '{}' (orden={}) del step '{}'.",
                taskName, currentTask.getStepTask().getTaskOrder(),
                stepRecord.getFlowStep().getStep().getName());

        TaskResult result = taskExecutorService.executeTask(fluxyTask, ctx);

        currentTask.setResult(result);
        currentTask.setStatus(TaskStatus.FINISHED);
        taskRecordRepository.save(currentTask);

        log.debug("[Fluxy] Tarea '{}' finalizada con resultado: {}", taskName, result);
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    private String computeIdempotencyKey(ExecutionContextRequest request) {
        String input = request.references().stream()
                .sorted(Comparator.comparing(r -> r.type()))
                .map(r -> r.type() + "=" + r.value())
                .reduce((a, b) -> a + ";" + b)
                .orElse("");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    // ── Conversions ──────────────────────────────────────────────────────────

    private ExecutionContext toExecutionContext(FluxyExecutionEntity entity, ExecutionContextRequest request) {
        ExecutionContextEntity ctxEntity = entity.getContext();
        ExecutionContextProxy ctx = new ExecutionContextProxy(ctxEntity.getType(), ctxEntity.getVersion(), objectMapper);

        // Cargar variables y referencias persistidas
        for (var v : ctxEntity.getVariables()) {
            ctx.addParameter(v.getName(), v.getValue());
        }
        for (var r : ctxEntity.getReferences()) {
            ctx.addReference(r.getRefType(), r.getValue());
        }

        // Inyectar variables adicionales del request (si las hay)
        if (request != null && request.variables() != null) {
            request.variables().forEach(v -> ctx.addParameter(v.name(), v.value()));
        }
        return ctx;
    }

    private ExecutionContext toSimpleExecutionContext(ExecutionContextRequest request) {
        ExecutionContextProxy ctx = new ExecutionContextProxy(
                request != null && request.type() != null ? request.type() : "default",
                request != null && request.version() != null ? request.version() : "1.0",
                objectMapper);
        if (request != null && request.variables() != null) {
            request.variables().forEach(v -> ctx.addParameter(v.name(), v.value()));
        }
        if (request != null && request.references() != null) {
            request.references().forEach(r -> ctx.addReference(r.type(), r.value()));
        }
        return ctx;
    }

    private FlowExecutionResultDto toFlowResult(FluxyExecutionEntity entity) {
        List<ExecutionStepRecordEntity> stepRecords =
                stepRecordRepository.findByExecutionOrderByFlowStepStepOrderAsc(entity);

        List<FlowStepExecutionDto> stepDtos = stepRecords.stream()
                .map(sr -> {
                    List<ExecutionTaskRecordEntity> taskRecs =
                            taskRecordRepository.findByExecutionStepRecord(sr);
                    List<TaskExecutionDto> taskDtos = taskRecs.stream()
                            .sorted(Comparator.comparing(tr -> tr.getStepTask().getTaskOrder()))
                            .map(tr -> new TaskExecutionDto(
                                    tr.getStepTask().getTask().getName(),
                                    tr.getStepTask().getTaskOrder(),
                                    tr.getStatus() != null ? tr.getStatus().name() : null,
                                    tr.getResult() != null ? tr.getResult().name() : null))
                            .toList();
                    return new FlowStepExecutionDto(
                            sr.getFlowStep().getStep().getId(),
                            sr.getFlowStep().getStep().getName(),
                            sr.getFlowStep().getStepOrder(),
                            sr.getStepStatus() != null ? sr.getStepStatus().name() : null,
                            taskDtos);
                })
                .toList();

        return new FlowExecutionResultDto(
                entity.getId(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getFlow().getId(),
                entity.getFlow().getName(),
                entity.getFlow().getType(),
                stepDtos);
    }
}

