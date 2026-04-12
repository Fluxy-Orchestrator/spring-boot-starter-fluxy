package org.fluxy.starter.registration;

import lombok.RequiredArgsConstructor;
import org.fluxy.core.model.FluxyTask;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

/**
 * Registro en memoria de todas las instancias de {@link FluxyTask} disponibles
 * en el contexto de Spring. Permite buscar tareas por nombre o por nombre de bean.
 *
 * <p>Al ser un componente del starter, el registro se actualiza automáticamente
 * con cada bean de tipo {@code FluxyTask} que esté en el contexto.</p>
 */
@RequiredArgsConstructor
public class FluxyTaskRegistry {

    private final ApplicationContext applicationContext;

    /**
     * Devuelve todos los beans de tipo {@link FluxyTask} del contexto,
     * indexados por nombre de bean.
     */
    public Map<String, FluxyTask> getAll() {
        return applicationContext.getBeansOfType(FluxyTask.class);
    }

    /**
     * Busca una tarea por su campo {@code name} (el valor que retorna {@code task.getName()}).
     *
     * @param name nombre de la tarea (atributo {@code name()} de la anotación {@code @Task})
     */
    public Optional<FluxyTask> findByName(String name) {
        return getAll().values().stream()
                .filter(task -> name.equals(task.getName()))
                .findFirst();
    }

    /**
     * Busca una tarea por el nombre de su bean en el contexto de Spring.
     *
     * @param beanName nombre del bean en el ApplicationContext
     */
    public Optional<FluxyTask> findByBeanName(String beanName) {
        return Optional.ofNullable(getAll().get(beanName));
    }

    /** Indica si existe una tarea con el nombre dado. */
    public boolean containsName(String name) {
        return findByName(name).isPresent();
    }
}

