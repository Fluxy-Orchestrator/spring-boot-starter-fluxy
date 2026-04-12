package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el estado de ejecución de un step dentro de un flow.
 *
 * @param stepId     identificador del step
 * @param stepName   nombre del step
 * @param order      orden del step dentro del flow
 * @param stepStatus estado de ejecución del step (PENDING, RUNNING, FINISHED)
 * @param tasks      estado de ejecución de cada tarea del step
 */
public record FlowStepExecutionDto(
        UUID stepId,
        String stepName,
        int order,
        String stepStatus,
        List<TaskExecutionDto> tasks
) {
}

