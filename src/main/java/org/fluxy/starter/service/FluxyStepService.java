package org.fluxy.starter.service;

import lombok.RequiredArgsConstructor;
import org.fluxy.core.model.TaskStatus;
import org.fluxy.spring.persistence.entity.FluxyStepEntity;
import org.fluxy.spring.persistence.entity.FluxyTaskEntity;
import org.fluxy.spring.persistence.entity.StepTaskEntity;
import org.fluxy.spring.persistence.repository.FluxyStepRepository;
import org.fluxy.spring.persistence.repository.FluxyTaskRepository;
import org.fluxy.spring.persistence.repository.StepTaskRepository;
import org.fluxy.starter.dto.AddTaskToStepRequest;
import org.fluxy.starter.dto.CreateFluxyStepRequest;
import org.fluxy.starter.dto.FluxyStepDto;
import org.fluxy.starter.dto.StepTaskDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de negocio para gestionar FluxyStep y sus tareas asignadas.
 */
@RequiredArgsConstructor
public class FluxyStepService {

    private final FluxyStepRepository fluxyStepRepository;
    private final StepTaskRepository stepTaskRepository;
    private final FluxyTaskRepository fluxyTaskRepository;

    /** Devuelve todos los steps registrados. */
    public List<FluxyStepDto> findAll() {
        return fluxyStepRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /** Busca un step por su ID. */
    public Optional<FluxyStepDto> findById(UUID id) {
        return fluxyStepRepository.findById(id).map(this::toDto);
    }

    /** Busca un step por su nombre. */
    public Optional<FluxyStepDto> findByName(String name) {
        return fluxyStepRepository.findByName(name).map(this::toDto);
    }

    /**
     * Crea un nuevo step en la base de datos.
     *
     * @throws IllegalArgumentException si ya existe un step con ese nombre
     */
    public FluxyStepDto create(CreateFluxyStepRequest request) {
        fluxyStepRepository.findByName(request.name()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Ya existe un step con el nombre '" + request.name() + "'");
        });
        FluxyStepEntity entity = new FluxyStepEntity();
        entity.setName(request.name());
        return toDto(fluxyStepRepository.save(entity));
    }

    /**
     * Agrega una tarea a un step existente.
     *
     * @throws IllegalArgumentException si el step o la tarea no existen
     */
    public StepTaskDto addTask(UUID stepId, AddTaskToStepRequest request) {
        FluxyStepEntity step = fluxyStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepId));
        FluxyTaskEntity task = fluxyTaskRepository.findById(request.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + request.taskId()));

        StepTaskEntity stepTask = new StepTaskEntity();
        stepTask.setStep(step);
        stepTask.setTask(task);
        stepTask.setTaskOrder(request.order());
        stepTask.setStatus(TaskStatus.PENDING);
        return toStepTaskDto(stepTaskRepository.save(stepTask));
    }

    /**
     * Elimina una tarea de un step.
     * Si la tarea no está en el step, la operación no hace nada.
     */
    public void removeTask(UUID stepId, UUID taskId) {
        FluxyStepEntity step = fluxyStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + stepId));
        stepTaskRepository.findByStep(step).stream()
                .filter(st -> st.getTask().getId().equals(taskId))
                .findFirst()
                .ifPresent(stepTaskRepository::delete);
    }

    /** Elimina un step por su ID. */
    public void delete(UUID id) {
        if (!fluxyStepRepository.existsById(id)) {
            throw new IllegalArgumentException("Step no encontrado con ID: " + id);
        }
        fluxyStepRepository.deleteById(id);
    }

    // ── Mapeo ────────────────────────────────────────────────────────────────

    private FluxyStepDto toDto(FluxyStepEntity entity) {
        List<StepTaskDto> tasks = entity.getTasks().stream()
                .map(this::toStepTaskDto)
                .toList();
        return new FluxyStepDto(entity.getId(), entity.getName(), tasks);
    }

    private StepTaskDto toStepTaskDto(StepTaskEntity entity) {
        return new StepTaskDto(
                entity.getId(),
                entity.getTask().getName(),
                entity.getTaskOrder(),
                entity.getStatus() != null ? entity.getStatus().name() : null
        );
    }
}

