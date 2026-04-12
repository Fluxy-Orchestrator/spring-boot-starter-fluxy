package org.fluxy.starter.dto;

/**
 * DTO de respuesta para el resultado de la ejecución de una tarea individual.
 *
 * @param taskName nombre de la tarea ejecutada
 * @param status   estado final de la tarea (FINISHED)
 * @param result   resultado de la ejecución (SUCCESS, FAILURE)
 */
public record TaskExecutionResultDto(
        String taskName,
        String status,
        String result
) {
}

