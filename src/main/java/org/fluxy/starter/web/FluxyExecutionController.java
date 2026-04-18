package org.fluxy.starter.web;

import lombok.RequiredArgsConstructor;
import org.fluxy.starter.dto.ExecutionContextRequest;
import org.fluxy.starter.dto.FlowExecutionResultDto;
import org.fluxy.starter.dto.StepExecutionResultDto;
import org.fluxy.starter.dto.TaskExecutionResultDto;
import org.fluxy.starter.service.FluxyExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la ejecucion de flows, steps y tasks de Fluxy.
 *
 * <p>Expone endpoints bajo {@code /fluxy/execution} para:</p>
 * <ul>
 *   <li>Inicializar una ejecucion de flow (con validacion de idempotencia)</li>
 *   <li>Procesar el siguiente step de una ejecucion por executionId</li>
 *   <li>Consultar el estado de una ejecucion</li>
 *   <li>Procesar un step especifico a demanda</li>
 *   <li>Ejecutar una tarea especifica a demanda por nombre</li>
 * </ul>
 */
@RestController
@RequestMapping("/fluxy/execution")
@RequiredArgsConstructor
public class FluxyExecutionController {

    private final FluxyExecutionService fluxyExecutionService;

    // ── Flow initialization — por ID ────────────────────────────────────────

    /**
     * POST /fluxy/execution/flows/{id}/initialize
     * Inicializa una ejecucion de un flow por ID. El body es obligatorio
     * y debe contener al menos una referencia. Si ya existe una ejecucion
     * activa con las mismas referencias, retorna la existente (idempotente).
     */
    @PostMapping("/flows/{id}/initialize")
    public ResponseEntity<FlowExecutionResultDto> initializeFlow(
            @PathVariable UUID id,
            @RequestBody ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.initializeFlow(id, request);
        return ResponseEntity.ok(result);
    }

    // ── Flow initialization — por nombre ────────────────────────────────────

    /**
     * POST /fluxy/execution/flows/name/{name}/initialize
     * Inicializa una ejecucion de un flow por nombre.
     */
    @PostMapping("/flows/name/{name}/initialize")
    public ResponseEntity<FlowExecutionResultDto> initializeFlowByName(
            @PathVariable String name,
            @RequestBody ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.initializeFlowByName(name, request);
        return ResponseEntity.ok(result);
    }

    // ── Execution processing — por executionId ──────────────────────────────

    /**
     * POST /fluxy/execution/executions/{executionId}/process
     * Procesa el siguiente step de una ejecucion. Este es el endpoint
     * principal: solo requiere el executionId para determinar el flow
     * y contexto a utilizar.
     */
    @PostMapping("/executions/{executionId}/process")
    public ResponseEntity<FlowExecutionResultDto> processExecution(
            @PathVariable UUID executionId,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.processExecution(executionId, request);
        return ResponseEntity.ok(result);
    }

    // ── Execution query ─────────────────────────────────────────────────────

    /**
     * GET /fluxy/execution/executions/{executionId}
     * Consulta el estado actual de una ejecucion.
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<FlowExecutionResultDto> getExecution(
            @PathVariable UUID executionId) {
        FlowExecutionResultDto result = fluxyExecutionService.getExecution(executionId);
        return ResponseEntity.ok(result);
    }

    // ── Flow processing — por ID (DEPRECATED) ──────────────────────────────

    /**
     * POST /fluxy/execution/flows/{id}/process
     * @deprecated Usar {@code POST /fluxy/execution/executions/{executionId}/process}.
     *             Este endpoint es ambiguo cuando hay multiples ejecuciones activas del mismo flow.
     */
    @Deprecated(forRemoval = true)
    @PostMapping("/flows/{id}/process")
    public ResponseEntity<FlowExecutionResultDto> processFlow(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.processExecution(id, request);
        return ResponseEntity.ok(result);
    }

    // ── Flow processing — por nombre (DEPRECATED) ──────────────────────────

    /**
     * POST /fluxy/execution/flows/name/{name}/process
     * @deprecated Usar {@code POST /fluxy/execution/executions/{executionId}/process}.
     */
    @Deprecated(forRemoval = true)
    @PostMapping("/flows/name/{name}/process")
    public ResponseEntity<FlowExecutionResultDto> processFlowByName(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.processFlowByName(name, request);
        return ResponseEntity.ok(result);
    }

    // ── Step — por ID ─────────────────────────────────────────────────────────

    /**
     * POST /fluxy/execution/steps/{id}/process
     * Procesa un step por ID (fuera del contexto de una ejecucion).
     */
    @PostMapping("/steps/{id}/process")
    public ResponseEntity<StepExecutionResultDto> processStep(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        StepExecutionResultDto result = fluxyExecutionService.processStep(id, request);
        return ResponseEntity.ok(result);
    }

    // ── Step — por nombre ─────────────────────────────────────────────────────

    /**
     * POST /fluxy/execution/steps/name/{name}/process
     * Procesa un step por nombre.
     */
    @PostMapping("/steps/name/{name}/process")
    public ResponseEntity<StepExecutionResultDto> processStepByName(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        StepExecutionResultDto result = fluxyExecutionService.processStepByName(name, request);
        return ResponseEntity.ok(result);
    }

    // ── Task — por nombre ─────────────────────────────────────────────────────

    /**
     * POST /fluxy/execution/tasks/{name}/execute
     * Ejecuta una tarea por nombre (mayor version disponible).
     */
    @PostMapping("/tasks/{name}/execute")
    public ResponseEntity<TaskExecutionResultDto> executeTask(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        TaskExecutionResultDto result = fluxyExecutionService.executeTask(name, request);
        return ResponseEntity.ok(result);
    }

    // ── Task — por nombre y version ────────────────────────────────────────────

    /**
     * POST /fluxy/execution/tasks/{name}/v/{version}/execute
     * Ejecuta una tarea por nombre y version exacta.
     */
    @PostMapping("/tasks/{name}/v/{version}/execute")
    public ResponseEntity<TaskExecutionResultDto> executeTaskVersioned(
            @PathVariable String name,
            @PathVariable int version,
            @RequestBody(required = false) ExecutionContextRequest request) {
        TaskExecutionResultDto result = fluxyExecutionService.executeTask(name, version, request);
        return ResponseEntity.ok(result);
    }
}
