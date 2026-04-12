package org.fluxy.starter.exception;

import java.util.List;

/**
 * Excepción lanzada cuando se detectan tareas en la base de datos que ya no
 * tienen una clase anotada con {@code @Task} en el código fuente.
 *
 * <p>Solo se lanza cuando {@code fluxy.task.registration.cleanup-stale.mode}
 * está configurado como {@code FAIL}.</p>
 */
public class StaleTaskException extends RuntimeException {

    private final List<String> staleTaskNames;

    public StaleTaskException(List<String> staleTaskNames) {
        super(buildMessage(staleTaskNames));
        this.staleTaskNames = List.copyOf(staleTaskNames);
    }

    public List<String> getStaleTaskNames() {
        return staleTaskNames;
    }

    private static String buildMessage(List<String> names) {
        return "[Fluxy] Se detectaron %d tarea(s) en la base de datos sin clase @Task en el código: %s"
                .formatted(names.size(), names);
    }
}

