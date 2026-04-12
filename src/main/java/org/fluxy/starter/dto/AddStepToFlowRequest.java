package org.fluxy.starter.dto;

import java.util.UUID;

/**
 * Request para agregar un FluxyStep a un FluxyFlow.
 *
 * @param stepId ID del step a agregar
 * @param order  orden de ejecución dentro del flow
 */
public record AddStepToFlowRequest(UUID stepId, int order) {
}

