package org.fluxy.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades para configurar el comportamiento del registro y validación
 * de tareas ({@code @Task}) al arrancar la aplicación.
 *
 * <p>Ejemplo en {@code application.yml}:</p>
 * <pre>{@code
 * fluxy:
 *   task:
 *     registration:
 *       auto-register: true
 *       cleanup-stale:
 *         enabled: false
 *         mode: WARN
 *       validate-steps: false
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "fluxy.task.registration")
public class FluxyTaskRegistrationProperties {

    /**
     * Activa el auto-registro en base de datos de los beans {@code @Task} que
     * extiendan {@code FluxyTask}. Cuando es {@code false}, las tareas se
     * instancian en el contexto de Spring pero <b>no</b> se persisten en BD.
     * Las validaciones de consistencia (cleanup-stale y validate-steps) siguen
     * ejecutándose de forma independiente.
     *
     * <p>Default: {@code true}.</p>
     */
    private boolean autoRegister = true;

    /** Configuración de limpieza de tareas obsoletas en base de datos. */
    private CleanupStale cleanupStale = new CleanupStale();

    /**
     * Activa la validación de versiones de tareas en los steps de la base de
     * datos. Cuando está habilitado, al arrancar se verifica que cada tarea
     * referenciada por un step exista en BD con la misma versión registrada
     * en el código:
     * <ul>
     *   <li>Si existe una versión más nueva en BD (pero no la registrada en
     *       el código) se emite un {@code WARN}.</li>
     *   <li>Si la tarea no existe en BD o solo existe con una versión anterior
     *       a la registrada en el step, se lanza
     *       {@code StepTaskVersionMismatchException}.</li>
     * </ul>
     *
     * <p>Default: {@code false}.</p>
     */
    private boolean validateSteps = false;

    @Data
    public static class CleanupStale {

        /**
         * Activa la detección de tareas huérfanas: tareas que existen en la base
         * de datos pero cuyo {@code @Task} correspondiente ya no se encuentra en
         * el código.
         *
         * <p>Default: {@code false}.</p>
         */
        private boolean enabled = false;

        /**
         * Modo de manejo cuando se detectan tareas huérfanas.
         * <ul>
         *   <li>{@code WARN} — loguea un warning por cada tarea huérfana.</li>
         *   <li>{@code FAIL} — lanza {@code StaleTaskException} deteniendo el arranque.</li>
         * </ul>
         *
         * <p>Default: {@code WARN}.</p>
         */
        private StaleHandlingMode mode = StaleHandlingMode.WARN;
    }

    /**
     * Modo de manejo de tareas obsoletas detectadas por {@code cleanup-stale}.
     */
    public enum StaleHandlingMode {
        /** Emite un log de nivel WARN por cada tarea huérfana. */
        WARN,
        /** Lanza {@code StaleTaskException} impidiendo el arranque de la aplicación. */
        FAIL
    }
}

