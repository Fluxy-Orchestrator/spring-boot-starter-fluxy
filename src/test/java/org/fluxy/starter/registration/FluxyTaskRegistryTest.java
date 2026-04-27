package org.fluxy.starter.registration;

import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.model.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluxyTaskRegistryTest {

    @Mock
    private ApplicationContext applicationContext;

    private FluxyTaskRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FluxyTaskRegistry(applicationContext);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private FluxyTask createTask(String taskName, int taskVersion) {
        return new FluxyTask() {
            {
                this.name = taskName;
                this.version = taskVersion;
                this.description = taskName + " v" + taskVersion;
            }

            @Override
            public TaskResult execute(ExecutionContext executionContext) {
                return TaskResult.SUCCESS;
            }
        };
    }

    // ── findLatestByName ────────────────────────────────────────────────────

    @Test
    void findLatestByName_returnsHighestVersion() {
        FluxyTask v1 = createTask("send-email", 1);
        FluxyTask v2 = createTask("send-email", 2);
        FluxyTask v3 = createTask("send-email", 3);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("email-v1", v1, "email-v2", v2, "email-v3", v3));

        Optional<FluxyTask> result = registry.findLatestByName("send-email");

        assertTrue(result.isPresent());
        assertEquals(3, result.get().getVersion());
    }

    @Test
    void findLatestByName_returnsEmpty_whenNotFound() {
        when(applicationContext.getBeansOfType(FluxyTask.class)).thenReturn(Map.of());

        Optional<FluxyTask> result = registry.findLatestByName("nonexistent");

        assertTrue(result.isEmpty());
    }

    // ── findByNameAndVersion ────────────────────────────────────────────────

    @Test
    void findByNameAndVersion_returnsExactMatch() {
        FluxyTask v1 = createTask("send-email", 1);
        FluxyTask v2 = createTask("send-email", 2);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("email-v1", v1, "email-v2", v2));

        Optional<FluxyTask> result = registry.findByNameAndVersion("send-email", 1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getVersion());
    }

    @Test
    void findByNameAndVersion_returnsEmpty_whenVersionNotFound() {
        FluxyTask v1 = createTask("send-email", 1);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("email-v1", v1));

        Optional<FluxyTask> result = registry.findByNameAndVersion("send-email", 99);

        assertTrue(result.isEmpty());
    }

    // ── findByBeanName ──────────────────────────────────────────────────────

    @Test
    void findByBeanName_returnsTask_whenBeanExists() {
        FluxyTask task = createTask("send-email", 1);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("myEmailBean", task));

        Optional<FluxyTask> result = registry.findByBeanName("myEmailBean");

        assertTrue(result.isPresent());
        assertEquals("send-email", result.get().getName());
    }

    @Test
    void findByBeanName_returnsEmpty_whenBeanNotFound() {
        when(applicationContext.getBeansOfType(FluxyTask.class)).thenReturn(Map.of());

        assertTrue(registry.findByBeanName("nonexistent").isEmpty());
    }

    // ── containsName / containsNameAndVersion ───────────────────────────────

    @Test
    void containsName_returnsTrue_whenExists() {
        FluxyTask task = createTask("send-email", 1);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("email", task));

        assertTrue(registry.containsName("send-email"));
        assertFalse(registry.containsName("nonexistent"));
    }

    @Test
    void containsNameAndVersion_returnsTrue_whenExactMatch() {
        FluxyTask task = createTask("send-email", 2);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("email", task));

        assertTrue(registry.containsNameAndVersion("send-email", 2));
        assertFalse(registry.containsNameAndVersion("send-email", 1));
    }

    // ── getAll ──────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsAllBeans() {
        FluxyTask t1 = createTask("task-a", 1);
        FluxyTask t2 = createTask("task-b", 1);
        when(applicationContext.getBeansOfType(FluxyTask.class))
                .thenReturn(Map.of("a", t1, "b", t2));

        Map<String, FluxyTask> all = registry.getAll();

        assertEquals(2, all.size());
    }
}



