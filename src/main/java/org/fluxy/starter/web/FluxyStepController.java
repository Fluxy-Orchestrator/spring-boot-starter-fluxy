package org.fluxy.starter.web;

import lombok.RequiredArgsConstructor;
import org.fluxy.starter.dto.AddTaskToStepRequest;
import org.fluxy.starter.dto.CreateFluxyStepRequest;
import org.fluxy.starter.dto.FluxyStepDto;
import org.fluxy.starter.dto.StepTaskDto;
import org.fluxy.starter.service.FluxyStepService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de {@code FluxyStep}.
 *
 * <p>Expone endpoints bajo {@code /fluxy/steps} para listar, buscar, crear
 * y eliminar steps, así como gestionar las tareas asignadas a cada step.</p>
 */
@RestController
@RequestMapping("/fluxy/steps")
@RequiredArgsConstructor
public class FluxyStepController {

    private final FluxyStepService fluxyStepService;

    /**
     * GET /fluxy/steps
     * Devuelve todos los steps registrados.
     */
    @GetMapping
    public ResponseEntity<List<FluxyStepDto>> findAll() {
        return ResponseEntity.ok(fluxyStepService.findAll());
    }

    /**
     * GET /fluxy/steps/{id}
     * Devuelve un step por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FluxyStepDto> findById(@PathVariable UUID id) {
        return fluxyStepService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /fluxy/steps/name/{name}
     * Devuelve un step por su nombre.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<FluxyStepDto> findByName(@PathVariable String name) {
        return fluxyStepService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /fluxy/steps
     * Crea un nuevo step.
     */
    @PostMapping
    public ResponseEntity<FluxyStepDto> create(@RequestBody CreateFluxyStepRequest request) {
        FluxyStepDto created = fluxyStepService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /fluxy/steps/{id}/tasks
     * Agrega una tarea a un step existente.
     */
    @PostMapping("/{id}/tasks")
    public ResponseEntity<StepTaskDto> addTask(
            @PathVariable UUID id,
            @RequestBody AddTaskToStepRequest request) {
        StepTaskDto result = fluxyStepService.addTask(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * DELETE /fluxy/steps/{stepId}/tasks/{taskId}
     * Elimina una tarea de un step.
     */
    @DeleteMapping("/{stepId}/tasks/{taskId}")
    public ResponseEntity<Void> removeTask(
            @PathVariable UUID stepId,
            @PathVariable UUID taskId) {
        fluxyStepService.removeTask(stepId, taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /fluxy/steps/{id}
     * Elimina un step por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fluxyStepService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

