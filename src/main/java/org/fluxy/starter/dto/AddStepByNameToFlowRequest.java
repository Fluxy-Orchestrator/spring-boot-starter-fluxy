package org.fluxy.starter.dto;

/**
 * Request para agregar un FluxyStep a un FluxyFlow usando el nombre del step.
 *
 * @param stepName nombre del step a agregar
 * @param order    orden de ejecución dentro del flow
 */
public record AddStepByNameToFlowRequest(String stepName, int order) {
}

