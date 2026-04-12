package org.fluxy.starter.web;

import lombok.RequiredArgsConstructor;
import org.fluxy.starter.dto.CreateFluxyTaskRequest;
import org.fluxy.starter.dto.FluxyTaskDto;
import org.fluxy.starter.service.FluxyTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de {@code FluxyTask}.
 *
 * <p>Expone endpoints bajo {@code /fluxy/tasks} para listar, buscar,
 * crear y eliminar tareas registradas en la base de datos.
 * Los endpoints de eliminación admiten identificación por ID o por nombre.</p>
 */
@RestController
@RequestMapping("/fluxy/tasks")
@RequiredArgsConstructor
public class FluxyTaskController {

    private final FluxyTaskService fluxyTaskService;

    /**
     * GET /fluxy/tasks
     * Devuelve todas las tareas registradas.
     */
    @GetMapping
    public ResponseEntity<List<FluxyTaskDto>> findAll() {
        return ResponseEntity.ok(fluxyTaskService.findAll());
    }

    /**
     * GET /fluxy/tasks/{id}
     * Devuelve una tarea por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FluxyTaskDto> findById(@PathVariable UUID id) {
        return fluxyTaskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /fluxy/tasks/name/{name}
     * Devuelve una tarea por su nombre.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<FluxyTaskDto> findByName(@PathVariable String name) {
        return fluxyTaskService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /fluxy/tasks
     * Crea una nueva tarea manualmente.
     */
    @PostMapping
    public ResponseEntity<FluxyTaskDto> create(@RequestBody CreateFluxyTaskRequest request) {
        FluxyTaskDto created = fluxyTaskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * DELETE /fluxy/tasks/{id}
     * Elimina una tarea por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fluxyTaskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /fluxy/tasks/name/{name}
     * Elimina una tarea por su nombre.
     */
    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteByName(@PathVariable String name) {
        fluxyTaskService.deleteByName(name);
        return ResponseEntity.noContent().build();
    }
}
