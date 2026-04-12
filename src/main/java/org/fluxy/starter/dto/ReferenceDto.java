package org.fluxy.starter.dto;

/**
 * DTO para representar una referencia del contexto de ejecución.
 *
 * @param type  tipo de la referencia
 * @param value valor de la referencia
 */
public record ReferenceDto(String type, String value) {
}

