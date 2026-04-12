package org.fluxy.starter.registration;

import lombok.RequiredArgsConstructor;
import org.fluxy.core.model.FluxyTask;
import org.springframework.context.ApplicationContext;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Registro en memoria de todas las instancias de {@link FluxyTask} disponibles
 * en el contexto de Spring. Permite buscar tareas por nombre, por nombre y
 * versión, o por nombre de bean.
 *
 * <p>Al ser un componente del starter, el registro se actualiza automáticamente
 * con cada bean de tipo {@code FluxyTask} que esté en el contexto.</p>
 *
 * <p>La versión de cada bean se obtiene directamente de {@link FluxyTask#getVersion()}.
 * Cuando coexisten varios beans con el mismo nombre pero distinta versión (e.g.
 * durante una migración), {@link #findLatestByName(String)} devuelve el de mayor
 * versión.</p>
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

    // ── Búsqueda por nombre ──────────────────────────────────────────────────

    /**
     * Busca una tarea por su campo {@code name} (el valor que retorna {@code task.getName()}).
     * Si existen varias versiones con el mismo nombre, devuelve la primera encontrada
     * (orden indeterminado). Prefiera {@link #findLatestByName(String)} o
     * {@link #findByNameAndVersion(String, int)} para un resultado determinista.
     *
     * @param name nombre de la tarea (atributo {@code name()} de la anotación {@code @Task})
     * @deprecated Usar {@link #findLatestByName(String)} o
     *             {@link #findByNameAndVersion(String, int)} en su lugar.
     */
    @Deprecated(forRemoval = true)
    public Optional<FluxyTask> findByName(String name) {
        return getAll().values().stream()
                .filter(task -> name.equals(task.getName()))
                .findFirst();
    }

    /**
     * Busca una tarea por nombre y devuelve la versión más alta registrada
     * en el contexto de Spring.
     *
     * <p>Si existen varios beans con el mismo nombre pero distinta versión,
     * se devuelve el de {@code getVersion()} más alto.</p>
     *
     * @param name nombre de la tarea
     */
    public Optional<FluxyTask> findLatestByName(String name) {
        return getAll().values().stream()
                .filter(task -> name.equals(task.getName()))
                .max(Comparator.comparingInt(FluxyTask::getVersion));
    }

    /**
     * Busca una tarea por nombre <b>y</b> versión exacta.
     *
     * @param name    nombre de la tarea
     * @param version versión esperada
     */
    public Optional<FluxyTask> findByNameAndVersion(String name, int version) {
        return getAll().values().stream()
                .filter(task -> name.equals(task.getName()))
                .filter(task -> task.getVersion() == version)
                .findFirst();
    }

    // ── Búsqueda por bean name ───────────────────────────────────────────────

    /**
     * Busca una tarea por el nombre de su bean en el contexto de Spring.
     *
     * @param beanName nombre del bean en el ApplicationContext
     */
    public Optional<FluxyTask> findByBeanName(String beanName) {
        return Optional.ofNullable(getAll().get(beanName));
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /** Indica si existe una tarea con el nombre dado (cualquier versión). */
    public boolean containsName(String name) {
        return findLatestByName(name).isPresent();
    }

    /** Indica si existe una tarea con el nombre y la versión dados. */
    public boolean containsNameAndVersion(String name, int version) {
        return findByNameAndVersion(name, version).isPresent();
    }
}

