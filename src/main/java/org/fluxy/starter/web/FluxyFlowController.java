package org.fluxy.starter.web;

import lombok.RequiredArgsConstructor;
import org.fluxy.starter.dto.AddStepByNameToFlowRequest;
import org.fluxy.starter.dto.AddStepToFlowRequest;
import org.fluxy.starter.dto.CreateFluxyFlowRequest;
import org.fluxy.starter.dto.FlowStepDto;
import org.fluxy.starter.dto.FluxyFlowDto;
import org.fluxy.starter.service.FluxyFlowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de {@code FluxyFlow}.
 *
 * <p>Expone endpoints bajo {@code /fluxy/flows} para listar, buscar, crear
 * y eliminar flows, así como gestionar los steps que componen cada flow.
 * Todos los endpoints de acción admiten identificación por ID o por nombre.</p>
 */
@RestController
@RequestMapping("/fluxy/flows")
@RequiredArgsConstructor
public class FluxyFlowController {

    private final FluxyFlowService fluxyFlowService;

    /**
     * GET /fluxy/flows
     * Devuelve todos los flows registrados.
     */
    @GetMapping
    public ResponseEntity<List<FluxyFlowDto>> findAll() {
        return ResponseEntity.ok(fluxyFlowService.findAll());
    }

    /**
     * GET /fluxy/flows/{id}
     * Devuelve un flow por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FluxyFlowDto> findById(@PathVariable UUID id) {
        return fluxyFlowService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /fluxy/flows/name/{name}
     * Devuelve un flow por su nombre.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<FluxyFlowDto> findByName(@PathVariable String name) {
        return fluxyFlowService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /fluxy/flows/type/{type}
     * Devuelve todos los flows de un tipo dado.
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<FluxyFlowDto>> findByType(@PathVariable String type) {
        return ResponseEntity.ok(fluxyFlowService.findByType(type));
    }

    /**
     * POST /fluxy/flows
     * Crea un nuevo flow.
     */
    @PostMapping
    public ResponseEntity<FluxyFlowDto> create(@RequestBody CreateFluxyFlowRequest request) {
        FluxyFlowDto created = fluxyFlowService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Agregar step ──────────────────────────────────────────────────────────

    /**
     * POST /fluxy/flows/{id}/steps
     * Agrega un step a un flow existente (flow por ID, step por ID).
     */
    @PostMapping("/{id}/steps")
    public ResponseEntity<FlowStepDto> addStep(
            @PathVariable UUID id,
            @RequestBody AddStepToFlowRequest request) {
        FlowStepDto result = fluxyFlowService.addStep(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * POST /fluxy/flows/name/{name}/steps
     * Agrega un step a un flow existente (flow por nombre, step por nombre).
     */
    @PostMapping("/name/{name}/steps")
    public ResponseEntity<FlowStepDto> addStepByName(
            @PathVariable String name,
            @RequestBody AddStepByNameToFlowRequest request) {
        FlowStepDto result = fluxyFlowService.addStepByName(name, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ── Eliminar step de flow ─────────────────────────────────────────────────

    /**
     * DELETE /fluxy/flows/{flowId}/steps/{stepId}
     * Elimina un step de un flow (por IDs).
     */
    @DeleteMapping("/{flowId}/steps/{stepId}")
    public ResponseEntity<Void> removeStep(
            @PathVariable UUID flowId,
            @PathVariable UUID stepId) {
        fluxyFlowService.removeStep(flowId, stepId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /fluxy/flows/name/{flowName}/steps/name/{stepName}
     * Elimina un step de un flow (por nombres).
     */
    @DeleteMapping("/name/{flowName}/steps/name/{stepName}")
    public ResponseEntity<Void> removeStepByName(
            @PathVariable String flowName,
            @PathVariable String stepName) {
        fluxyFlowService.removeStepByName(flowName, stepName);
        return ResponseEntity.noContent().build();
    }

    // ── Eliminar flow ─────────────────────────────────────────────────────────

    /**
     * DELETE /fluxy/flows/{id}
     * Elimina un flow por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fluxyFlowService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /fluxy/flows/name/{name}
     * Elimina un flow por su nombre.
     */
    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteByName(@PathVariable String name) {
        fluxyFlowService.deleteByName(name);
        return ResponseEntity.noContent().build();
    }
}
