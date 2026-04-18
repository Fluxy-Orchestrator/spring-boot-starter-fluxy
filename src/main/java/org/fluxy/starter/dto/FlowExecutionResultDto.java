package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el resultado de la ejecucion de un flow.
 *
 * @param executionId     identificador unico de la ejecucion
 * @param executionStatus estado de la ejecucion (PENDING, RUNNING, FINISHED)
 * @param flowId          identificador del flow
 * @param flowName        nombre del flow
 * @param flowType        tipo del flow
 * @param steps           estado de ejecucion de cada step del flow
 */
public record FlowExecutionResultDto(
        UUID executionId,
        String executionStatus,
        UUID flowId,
        String flowName,
        String flowType,
        List<FlowStepExecutionDto> steps
) {
}

