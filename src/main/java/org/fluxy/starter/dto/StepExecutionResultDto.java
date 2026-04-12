package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el resultado de la ejecución de un step.
 *
 * @param stepId   identificador del step
 * @param stepName nombre del step
 * @param tasks    estado de ejecución de cada tarea del step
 */
public record StepExecutionResultDto(
        UUID stepId,
        String stepName,
        List<TaskExecutionDto> tasks
) {
}

