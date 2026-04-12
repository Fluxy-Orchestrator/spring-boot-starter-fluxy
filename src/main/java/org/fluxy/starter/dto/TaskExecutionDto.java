package org.fluxy.starter.dto;

/**
 * DTO de respuesta para el estado de ejecución de una tarea.
 *
 * @param taskName nombre de la tarea
 * @param order    orden de la tarea dentro del step
 * @param status   estado de ejecución (PENDING, RUNNING, FINISHED)
 * @param result   resultado de la ejecución (SUCCESS, FAILURE) o null si no ha terminado
 */
public record TaskExecutionDto(
        String taskName,
        int order,
        String status,
        String result
) {
}

