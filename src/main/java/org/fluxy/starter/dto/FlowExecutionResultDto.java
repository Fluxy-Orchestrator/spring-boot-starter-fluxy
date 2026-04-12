package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el resultado de la ejecución de un flow.
 *
 * @param flowId   identificador del flow
 * @param flowName nombre del flow
 * @param flowType tipo del flow
 * @param steps    estado de ejecución de cada step del flow
 */
public record FlowExecutionResultDto(
        UUID flowId,
        String flowName,
        String flowType,
        List<FlowStepExecutionDto> steps
) {
}

