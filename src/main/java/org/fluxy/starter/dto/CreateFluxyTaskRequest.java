package org.fluxy.starter.dto;

/**
 * Request para crear una FluxyTask manualmente.
 *
 * @param name        nombre único de la tarea
 * @param version     versión de la tarea
 * @param description descripción de la tarea
 */
public record CreateFluxyTaskRequest(String name, int version, String description) {
}

