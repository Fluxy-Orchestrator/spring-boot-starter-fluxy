package org.fluxy.starter.exception;

/**
 * Excepción lanzada durante la validación de steps cuando una tarea
 * referenciada por un step en la base de datos no existe o tiene una
 * versión anterior a la requerida.
 *
 * <p>Escenarios en los que se lanza:</p>
 * <ul>
 *   <li>La tarea no existe en la base de datos (independientemente de la versión).</li>
 *   <li>La tarea existe en BD pero solo con versiones anteriores a la registrada
 *       en el step.</li>
 * </ul>
 *
 * <p>Solo se lanza cuando {@code fluxy.task.registration.validate-steps} es {@code true}.</p>
 */
public class StepTaskVersionMismatchException extends RuntimeException {

    private final String taskName;
    private final int expectedVersion;

    public StepTaskVersionMismatchException(String taskName, int expectedVersion, String detail) {
        super("[Fluxy] Incompatibilidad de versión para la tarea '%s' (v%d): %s"
                .formatted(taskName, expectedVersion, detail));
        this.taskName = taskName;
        this.expectedVersion = expectedVersion;
    }

    public String getTaskName() {
        return taskName;
    }

    public int getExpectedVersion() {
        return expectedVersion;
    }
}

