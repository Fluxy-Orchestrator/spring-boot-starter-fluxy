package org.fluxy.starter.service;

import lombok.RequiredArgsConstructor;
import org.fluxy.spring.persistence.entity.FluxyTaskEntity;
import org.fluxy.spring.persistence.repository.FluxyTaskRepository;
import org.fluxy.starter.dto.CreateFluxyTaskRequest;
import org.fluxy.starter.dto.FluxyTaskDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de negocio para gestionar FluxyTask en la base de datos.
 */
@RequiredArgsConstructor
public class FluxyTaskService {

    private final FluxyTaskRepository fluxyTaskRepository;

    /** Devuelve todas las tareas registradas. */
    public List<FluxyTaskDto> findAll() {
        return fluxyTaskRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /** Busca una tarea por su ID. */
    public Optional<FluxyTaskDto> findById(UUID id) {
        return fluxyTaskRepository.findById(id).map(this::toDto);
    }

    /** Busca una tarea por su nombre. */
    public Optional<FluxyTaskDto> findByName(String name) {
        return fluxyTaskRepository.findByName(name).map(this::toDto);
    }

    /**
     * Crea una nueva tarea en la base de datos.
     *
     * @throws IllegalArgumentException si ya existe una tarea con ese nombre
     */
    public FluxyTaskDto create(CreateFluxyTaskRequest request) {
        fluxyTaskRepository.findByName(request.name()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Ya existe una tarea con el nombre '" + request.name() + "'");
        });
        FluxyTaskEntity entity = new FluxyTaskEntity();
        entity.setName(request.name());
        entity.setVersion(request.version());
        entity.setDescription(request.description());
        return toDto(fluxyTaskRepository.save(entity));
    }

    /**
     * Garantiza que la tarea esté registrada en la base de datos con la versión
     * y descripción indicadas. Si no existe, la crea; si existe, actualiza
     * versión y descripción. Usado por el auto-registro de @Task.
     */
    public FluxyTaskEntity ensureRegistered(String name, int version, String description) {
        return fluxyTaskRepository.findByName(name)
                .map(existing -> {
                    existing.setVersion(version);
                    existing.setDescription(description);
                    return fluxyTaskRepository.save(existing);
                })
                .orElseGet(() -> {
                    FluxyTaskEntity entity = new FluxyTaskEntity();
                    entity.setName(name);
                    entity.setVersion(version);
                    entity.setDescription(description);
                    return fluxyTaskRepository.save(entity);
                });
    }

    /** Elimina una tarea por su ID. */
    public void delete(UUID id) {
        if (!fluxyTaskRepository.existsById(id)) {
            throw new IllegalArgumentException("Tarea no encontrada con ID: " + id);
        }
        fluxyTaskRepository.deleteById(id);
    }

    // ── Mapeo ────────────────────────────────────────────────────────────────

    private FluxyTaskDto toDto(FluxyTaskEntity entity) {
        return new FluxyTaskDto(entity.getId(), entity.getName(), entity.getVersion(), entity.getDescription());
    }
}

