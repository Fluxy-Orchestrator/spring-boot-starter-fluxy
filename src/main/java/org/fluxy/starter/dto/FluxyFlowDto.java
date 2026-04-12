package org.fluxy.starter.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para un FluxyFlow con sus steps asignados.
 */
public record FluxyFlowDto(UUID id, String name, String type, String description, List<FlowStepDto> steps) {
}

