package org.fluxy.starter.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.spring.annotation.Task;
import org.fluxy.spring.persistence.entity.FluxyTaskEntity;
import org.fluxy.spring.persistence.entity.StepTaskEntity;
import org.fluxy.spring.persistence.repository.FluxyTaskRepository;
import org.fluxy.spring.persistence.repository.StepTaskRepository;
import org.fluxy.starter.exception.StaleTaskException;
import org.fluxy.starter.exception.StepTaskVersionMismatchException;
import org.fluxy.starter.properties.FluxyTaskRegistrationProperties;
import org.fluxy.starter.service.FluxyTaskService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Procesador de auto-registro y validación de {@link FluxyTask}.
 *
 * <p>Al iniciar la aplicación, detecta todos los beans del contexto que:
 * <ul>
 *   <li>Extiendan {@link FluxyTask}</li>
 *   <li>Estén anotados con {@link Task}</li>
 * </ul>
 * y ejecuta las siguientes acciones según la configuración en
 * {@code fluxy.task.registration.*}:</p>
 *
 * <ol>
 *   <li><b>auto-register</b> — persiste en BD cada tarea detectada con nombre,
 *       versión y descripción (idempotente).</li>
 *   <li><b>cleanup-stale</b> — detecta tareas huérfanas en BD (sin clase en
 *       código) y emite WARN o lanza {@link StaleTaskException} según el modo.</li>
 *   <li><b>validate-steps</b> — valida que cada tarea referenciada por un step
 *       en BD exista con la versión correcta; emite WARN o lanza
 *       {@link StepTaskVersionMismatchException} según corresponda.</li>
 * </ol>
 *
 * <p>Utiliza {@link ApplicationReadyEvent} para garantizar que la capa JPA
 * esté completamente inicializada antes de operar con la base de datos.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TaskAutoRegistrationProcessor implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext applicationContext;
    private final FluxyTaskService fluxyTaskService;
    private final FluxyTaskRepository fluxyTaskRepository;
    private final StepTaskRepository stepTaskRepository;
    private final FluxyTaskRegistrationProperties properties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[Fluxy] Iniciando procesamiento de tareas @Task...");

        // ── 1. Recopilar beans @Task del contexto ────────────────────────────
        Map<String, TaskInfo> codeTasksByName = discoverAnnotatedTasks();

        // ── 2. Auto-registro en BD ───────────────────────────────────────────
        if (properties.isAutoRegister()) {
            performAutoRegistration(codeTasksByName);
        } else {
            log.info("[Fluxy] Auto-registro deshabilitado (fluxy.task.registration.auto-register=false). " +
                    "Las tareas NO se persisten en BD.");
        }

        // ── 3. Cleanup de tareas obsoletas ───────────────────────────────────
        if (properties.getCleanupStale().isEnabled()) {
            performStaleCleanup(codeTasksByName);
        }

        // ── 4. Validación de versiones en steps ──────────────────────────────
        if (properties.isValidateSteps()) {
            performStepValidation(codeTasksByName);
        }

        log.info("[Fluxy] Procesamiento de tareas completado.");
    }

    // ── Descubrimiento de beans ──────────────────────────────────────────────

    /**
     * Recorre todos los beans {@link FluxyTask} del contexto, resuelve la clase
     * real (detrás de un posible proxy CGLIB) y extrae los datos de la anotación
     * {@link Task}. Los beans sin {@code @Task} se omiten con un log debug.
     */
    private Map<String, TaskInfo> discoverAnnotatedTasks() {
        Map<String, FluxyTask> taskBeans = applicationContext.getBeansOfType(FluxyTask.class);
        Map<String, TaskInfo> result = new LinkedHashMap<>();

        for (Map.Entry<String, FluxyTask> entry : taskBeans.entrySet()) {
            Class<?> taskClass = resolveRealClass(entry.getValue());
            Task annotation = taskClass.getAnnotation(Task.class);

            if (annotation == null) {
                log.debug("[Fluxy] Bean '{}' extiende FluxyTask pero no tiene @Task; se omite.",
                        entry.getKey());
                continue;
            }

            result.put(annotation.name(), new TaskInfo(
                    annotation.name(),
                    annotation.version(),
                    annotation.description(),
                    taskClass
            ));
        }

        log.info("[Fluxy] Detectadas {} tarea(s) @Task en el contexto.", result.size());
        return result;
    }

    // ── Auto-registro ────────────────────────────────────────────────────────

    private void performAutoRegistration(Map<String, TaskInfo> codeTasksByName) {
        int registered = 0;
        for (TaskInfo info : codeTasksByName.values()) {
            try {
                fluxyTaskService.ensureRegistered(info.name(), info.version(), info.description());
                log.info("[Fluxy] ✓ Tarea auto-registrada: '{}' (v{}) → clase: {}",
                        info.name(), info.version(), info.taskClass().getSimpleName());
                registered++;
            } catch (Exception ex) {
                log.error("[Fluxy] ✗ Error al auto-registrar la tarea '{}': {}",
                        info.name(), ex.getMessage(), ex);
            }
        }
        log.info("[Fluxy] Auto-registro completado. {} tarea(s) procesada(s).", registered);
    }

    // ── Cleanup de tareas obsoletas ──────────────────────────────────────────

    /**
     * Compara las tareas registradas en BD con las detectadas en el código.
     * Cada tarea en BD cuyo nombre no corresponda a ningún bean {@code @Task}
     * se considera huérfana.
     */
    private void performStaleCleanup(Map<String, TaskInfo> codeTasksByName) {
        log.info("[Fluxy] Verificando tareas obsoletas en base de datos...");

        Set<String> codeNames = codeTasksByName.keySet();
        List<FluxyTaskEntity> allDbTasks = fluxyTaskRepository.findAll();

        List<String> staleNames = allDbTasks.stream()
                .map(FluxyTaskEntity::getName)
                .filter(dbName -> !codeNames.contains(dbName))
                .toList();

        if (staleNames.isEmpty()) {
            log.info("[Fluxy] No se detectaron tareas obsoletas en BD.");
            return;
        }

        FluxyTaskRegistrationProperties.StaleHandlingMode mode = properties.getCleanupStale().getMode();

        if (mode == FluxyTaskRegistrationProperties.StaleHandlingMode.FAIL) {
            log.error("[Fluxy] Se detectaron {} tarea(s) obsoleta(s) en BD (modo FAIL): {}",
                    staleNames.size(), staleNames);
            throw new StaleTaskException(staleNames);
        } else {
            for (String staleName : staleNames) {
                log.warn("[Fluxy] ⚠ Tarea obsoleta detectada en BD: '{}'. " +
                        "No existe un bean @Task correspondiente en el código.", staleName);
            }
        }
    }

    // ── Validación de versiones en steps ─────────────────────────────────────

    /**
     * Recorre todos los {@link StepTaskEntity} en la base de datos y para cada
     * tarea referenciada verifica la consistencia de versiones:
     * <ul>
     *   <li>Si la tarea del step existe en el código con la misma versión → OK.</li>
     *   <li>Si en BD existe una versión más nueva que la del código pero no la
     *       versión registrada en el step → WARN (hay una versión superior
     *       disponible pero el step apunta a una versión que ya no está en código).</li>
     *   <li>Si la tarea no existe en BD o solo con versiones anteriores a la del
     *       código → lanza {@link StepTaskVersionMismatchException}.</li>
     * </ul>
     */
    private void performStepValidation(Map<String, TaskInfo> codeTasksByName) {
        log.info("[Fluxy] Validando versiones de tareas en steps...");

        List<StepTaskEntity> allStepTasks = stepTaskRepository.findAll();

        if (allStepTasks.isEmpty()) {
            log.info("[Fluxy] No hay tareas asignadas a steps en BD. Validación omitida.");
            return;
        }

        // Agrupar por nombre de tarea para no repetir validaciones
        Map<String, List<StepTaskEntity>> stepTasksByTaskName = allStepTasks.stream()
                .collect(Collectors.groupingBy(st -> st.getTask().getName()));

        for (Map.Entry<String, List<StepTaskEntity>> entry : stepTasksByTaskName.entrySet()) {
            String taskName = entry.getKey();
            FluxyTaskEntity dbTask = entry.getValue().get(0).getTask();
            int dbVersion = dbTask.getVersion();

            TaskInfo codeTask = codeTasksByName.get(taskName);

            if (codeTask == null) {
                // La tarea del step no existe en el código → error
                throw new StepTaskVersionMismatchException(taskName, dbVersion,
                        "La tarea no existe en el código fuente. " +
                        "El step referencia una tarea que ya fue eliminada.");
            }

            int codeVersion = codeTask.version();

            if (codeVersion == dbVersion) {
                // Versiones coinciden → OK
                log.debug("[Fluxy] Tarea '{}' v{} en step: versión coincide con el código.",
                        taskName, dbVersion);
            } else if (codeVersion > dbVersion) {
                // El código tiene versión más nueva que la registrada en BD
                log.warn("[Fluxy] ⚠ La tarea '{}' tiene v{} en BD pero v{} en el código. " +
                                "Considere actualizar el registro en base de datos.",
                        taskName, dbVersion, codeVersion);
            } else {
                // dbVersion > codeVersion → la BD tiene versión superior al código
                throw new StepTaskVersionMismatchException(taskName, dbVersion,
                        "La versión en BD (v%d) es superior a la del código (v%d). "
                                .formatted(dbVersion, codeVersion) +
                        "Posible rollback de código detectado.");
            }
        }

        log.info("[Fluxy] Validación de steps completada exitosamente.");
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /**
     * Resuelve la clase real detrás de un posible proxy CGLIB de Spring.
     */
    private Class<?> resolveRealClass(Object bean) {
        Class<?> clazz = bean.getClass();
        if (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }

    /**
     * Información extraída de una anotación {@link Task} sobre un bean del contexto.
     */
    private record TaskInfo(String name, int version, String description, Class<?> taskClass) {
    }
}
