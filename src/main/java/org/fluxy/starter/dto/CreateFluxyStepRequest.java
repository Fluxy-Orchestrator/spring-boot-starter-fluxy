package org.fluxy.starter.dto;

/**
 * Request para crear un FluxyStep.
 *
 * @param name nombre único del step
 */
public record CreateFluxyStepRequest(String name) {
}

