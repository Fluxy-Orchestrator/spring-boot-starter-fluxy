package org.fluxy.starter.dto;

import java.util.UUID;

/**
 * DTO de respuesta para una FluxyTask registrada.
 */
public record FluxyTaskDto(UUID id, String name, int version, String description) {
}

