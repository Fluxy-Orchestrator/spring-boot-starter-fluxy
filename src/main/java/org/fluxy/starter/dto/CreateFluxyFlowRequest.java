package org.fluxy.starter.dto;

/**
 * Request para crear un FluxyFlow.
 *
 * @param name        nombre del flow
 * @param type        tipo del flow
 * @param description descripción del flow
 */
public record CreateFluxyFlowRequest(String name, String type, String description) {
}

