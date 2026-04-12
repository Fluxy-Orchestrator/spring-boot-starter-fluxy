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
 * Controlador REST para la ejecución de flows, steps y tasks de Fluxy.
 *
 * <p>Expone endpoints bajo {@code /fluxy/execution} para:</p>
 * <ul>
 *   <li>Inicializar la ejecución de un flow (resetear estados a PENDING)</li>
 *   <li>Procesar el siguiente step de un flow (ejecución step-by-step)</li>
 *   <li>Procesar un step específico a demanda</li>
 *   <li>Ejecutar una tarea específica a demanda por nombre</li>
 * </ul>
 *
 * <p>Todos los endpoints de flow y step admiten identificación por ID o por nombre.
 * Las ejecuciones de tareas se delegan al {@code TaskExecutorService} del core,
 * que publica eventos en el bus configurado (SPRING, SQS o RABBIT).</p>
 */
@RestController
@RequestMapping("/fluxy/execution")
@RequiredArgsConstructor
public class FluxyExecutionController {

    private final FluxyExecutionService fluxyExecutionService;

    // ── Flow — por ID ─────────────────────────────────────────────────────────

    /**
     * POST /fluxy/execution/flows/{id}/initialize
     * Inicializa un flow por ID.
     */
    @PostMapping("/flows/{id}/initialize")
    public ResponseEntity<FlowExecutionResultDto> initializeFlow(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.initializeFlow(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /fluxy/execution/flows/{id}/process
     * Procesa el siguiente step de un flow por ID.
     */
    @PostMapping("/flows/{id}/process")
    public ResponseEntity<FlowExecutionResultDto> processFlow(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.processFlow(id, request);
        return ResponseEntity.ok(result);
    }

    // ── Flow — por nombre ─────────────────────────────────────────────────────

    /**
     * POST /fluxy/execution/flows/name/{name}/initialize
     * Inicializa un flow por nombre.
     */
    @PostMapping("/flows/name/{name}/initialize")
    public ResponseEntity<FlowExecutionResultDto> initializeFlowByName(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.initializeFlowByName(name, request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /fluxy/execution/flows/name/{name}/process
     * Procesa el siguiente step de un flow por nombre.
     */
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
     * Procesa un step por ID.
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
     * Ejecuta una tarea por nombre. Si existen varias versiones con el mismo
     * nombre, se utiliza la de <b>mayor versión</b> disponible en el registro.
     */
    @PostMapping("/tasks/{name}/execute")
    public ResponseEntity<TaskExecutionResultDto> executeTask(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        TaskExecutionResultDto result = fluxyExecutionService.executeTask(name, request);
        return ResponseEntity.ok(result);
    }

    // ── Task — por nombre y versión ────────────────────────────────────────────

    /**
     * POST /fluxy/execution/tasks/{name}/v/{version}/execute
     * Ejecuta una tarea por nombre y versión exacta.
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
