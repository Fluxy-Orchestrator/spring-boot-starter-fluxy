package org.fluxy.starter.dto;

import java.util.UUID;

/**
 * DTO de respuesta para un Step dentro de un Flow.
 */
public record FlowStepDto(UUID id, UUID stepId, String stepName, int order, String stepStatus) {
}

