package org.fluxy.starter.support;

import org.fluxy.core.model.FluxyTask;
import org.fluxy.spring.annotation.Task;

/**
 * Clase base que simplifica la implementación de {@link FluxyTask} cuando la
 * clase concreta está anotada con {@link Task}.
 *
 * <p>Al extender {@code AbstractFluxyTask} <b>no es necesario</b> inicializar
 * {@code name}, {@code version} ni {@code description} en el constructor:
 * estos valores se leen automáticamente de la anotación {@code @Task} presente
 * en la subclase concreta.</p>
 *
 * <h3>Ejemplo de uso</h3>
 * <pre>{@code
 * @Task(name = "send-email", version = 2, description = "Envía un correo electrónico")
 * @Component
 * public class SendEmailTask extends AbstractFluxyTask {
 *
 *     @Override
 *     public TaskResult execute(ExecutionContext ctx) {
 *         // lógica de envío de correo
 *         return TaskResult.SUCCESS;
 *     }
 * }
 * }</pre>
 *
 * <p>Si la subclase <b>no</b> lleva {@code @Task}, el constructor lanza
 * {@link IllegalStateException} indicando la clase afectada.</p>
 */
public abstract class AbstractFluxyTask extends FluxyTask {

    /**
     * Inicializa {@code name}, {@code version} y {@code description} a partir
     * de la anotación {@link Task} de la subclase concreta.
     *
     * @throws IllegalStateException si la clase concreta no está anotada con {@code @Task}
     */
    protected AbstractFluxyTask() {
        Class<?> clazz = resolveRealClass(this.getClass());
        Task annotation = clazz.getAnnotation(Task.class);

        if (annotation == null) {
            throw new IllegalStateException(
                    "[Fluxy] La clase '%s' extiende AbstractFluxyTask pero no está anotada con @Task. "
                            .formatted(clazz.getName()) +
                    "Añada @Task(name = \"...\", version = ..., description = \"...\") a la clase, " +
                    "o extienda FluxyTask directamente e inicialice los campos en su constructor.");
        }

        this.name = annotation.name();
        this.version = annotation.version();
        this.description = annotation.description();
    }

    /**
     * Resuelve la clase real detrás de un posible proxy CGLIB de Spring.
     */
    private static Class<?> resolveRealClass(Class<?> clazz) {
        while (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }
}

