package org.fluxy.starter.dto;

/**
 * Request para agregar una FluxyTask a un FluxyStep usando el nombre de la tarea.
 *
 * @param taskName nombre de la tarea a agregar
 * @param order    orden de ejecución dentro del step
 */
public record AddTaskByNameToStepRequest(String taskName, int order) {
}

