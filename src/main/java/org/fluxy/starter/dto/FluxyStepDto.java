package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para un FluxyStep con sus tareas asignadas.
 */
public record FluxyStepDto(UUID id, String name, List<StepTaskDto> tasks) {
}

