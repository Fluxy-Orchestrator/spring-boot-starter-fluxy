package org.fluxy.starter.exception;


/**
 * Excepción lanzada cuando se detectan tareas en la base de datos que ya no
 * tienen una clase anotada con {@code @Task} en el código fuente.
 *
 * <p>Solo se lanza cuando {@code fluxy.task.registration.cleanup-stale.mode}
 * está configurado como {@code FAIL}.</p>
 */
public class UndefinedContextVariableException extends RuntimeException {

    public UndefinedContextVariableException(String message) {
        super(message);
    }
}

