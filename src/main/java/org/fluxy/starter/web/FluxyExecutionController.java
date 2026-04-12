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
 * <p>Las ejecuciones de tareas se delegan al {@code TaskExecutorService} del core,
 * que publica eventos en el bus configurado (SPRING, SQS o RABBIT).</p>
 */
@RestController
@RequestMapping("/fluxy/execution")
@RequiredArgsConstructor
public class FluxyExecutionController {

    private final FluxyExecutionService fluxyExecutionService;

    /**
     * POST /fluxy/execution/flows/{id}/initialize
     *
     * <p>Inicializa la ejecución de un flow: resetea todos los steps a PENDING
     * y todas las tareas de cada step a PENDING con resultado null.</p>
     *
     * @param id      identificador del flow a inicializar
     * @param request contexto de ejecución (opcional)
     * @return estado del flow tras la inicialización
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
     *
     * <p>Procesa el siguiente paso en un flow: encuentra el step en ejecución
     * (o el siguiente pendiente) y ejecuta su siguiente tarea pendiente.
     * Cuando todas las tareas de un step terminan, el step pasa a FINISHED.</p>
     *
     * @param id      identificador del flow a procesar
     * @param request contexto de ejecución con variables y referencias
     * @return estado actualizado del flow
     */
    @PostMapping("/flows/{id}/process")
    public ResponseEntity<FlowExecutionResultDto> processFlow(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        FlowExecutionResultDto result = fluxyExecutionService.processFlow(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /fluxy/execution/steps/{id}/process
     *
     * <p>Procesa un step específico a demanda: encuentra la siguiente tarea
     * pendiente y la ejecuta. No requiere que el step pertenezca a un flow
     * en ejecución.</p>
     *
     * @param id      identificador del step a procesar
     * @param request contexto de ejecución con variables y referencias
     * @return estado actualizado del step
     */
    @PostMapping("/steps/{id}/process")
    public ResponseEntity<StepExecutionResultDto> processStep(
            @PathVariable UUID id,
            @RequestBody(required = false) ExecutionContextRequest request) {
        StepExecutionResultDto result = fluxyExecutionService.processStep(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /fluxy/execution/tasks/{name}/execute
     *
     * <p>Ejecuta una tarea específica a demanda, buscándola por nombre en el
     * registro de tareas ({@code FluxyTaskRegistry}). La tarea debe existir
     * como bean {@code @Task} en el contexto de Spring.</p>
     *
     * @param name    nombre de la tarea a ejecutar
     * @param request contexto de ejecución con variables y referencias
     * @return resultado de la ejecución
     */
    @PostMapping("/tasks/{name}/execute")
    public ResponseEntity<TaskExecutionResultDto> executeTask(
            @PathVariable String name,
            @RequestBody(required = false) ExecutionContextRequest request) {
        TaskExecutionResultDto result = fluxyExecutionService.executeTask(name, request);
        return ResponseEntity.ok(result);
    }
}

