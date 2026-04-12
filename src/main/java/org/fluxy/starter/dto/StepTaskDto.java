package org.fluxy.starter.dto;

import java.util.UUID;

/**
 * DTO de respuesta para una tarea dentro de un step.
 */
public record StepTaskDto(UUID id, String taskName, int taskOrder, String status) {
}

