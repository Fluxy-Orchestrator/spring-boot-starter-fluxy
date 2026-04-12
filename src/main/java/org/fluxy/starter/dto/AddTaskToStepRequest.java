package org.fluxy.starter.dto;

import java.util.UUID;

/**
 * Request para agregar una FluxyTask a un FluxyStep.
 *
 * @param taskId ID de la tarea a agregar
 * @param order  orden de ejecución dentro del step
 */
public record AddTaskToStepRequest(UUID taskId, int order) {
}

