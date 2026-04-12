package org.fluxy.starter.dto;

/**
 * DTO para representar una variable del contexto de ejecución.
 *
 * @param name  nombre de la variable
 * @param value valor de la variable
 */
public record VariableDto(String name, String value) {
}

