package org.fluxy.starter.service;

import org.fluxy.core.model.*;
import org.fluxy.core.service.TaskExecutorService;
import org.fluxy.spring.persistence.entity.*;
import org.fluxy.spring.persistence.repository.*;
import org.fluxy.starter.dto.*;
import org.fluxy.starter.registration.FluxyTaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FluxyExecutionServiceTest {

    @Mock private FluxyFlowRepository flowRepository;
    @Mock private FlowStepRepository flowStepRepository;
    @Mock private FluxyStepRepository stepRepository;
    @Mock private StepTaskRepository stepTaskRepository;
    @Mock private FluxyExecutionRepository executionRepository;
    @Mock private ExecutionContextRepository contextRepository;
    @Mock private ExecutionStepRecordRepository stepRecordRepository;
    @Mock private ExecutionTaskRecordRepository taskRecordRepository;
    @Mock private FluxyTaskRegistry taskRegistry;
    @Mock private TaskExecutorService taskExecutorService;
    @Mock private ObjectMapper objectMapper;

    private FluxyExecutionService service;

    @BeforeEach
    void setUp() {
        service = new FluxyExecutionService(
                flowRepository, flowStepRepository, stepRepository, stepTaskRepository,
                executionRepository, contextRepository, stepRecordRepository,
                taskRecordRepository, taskRegistry, taskExecutorService, objectMapper
        );
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private FluxyFlowEntity createFlowEntity(String name) {
        FluxyFlowEntity flow = new FluxyFlowEntity();
        flow.setId(UUID.randomUUID());
        flow.setName(name);
        flow.setType("BATCH");
        return flow;
    }

    private ExecutionContextRequest createRequest(String type) {
        return new ExecutionContextRequest(
                type, "1.0",
                List.of(new VariableDto("key", "value")),
                List.of(new ReferenceDto("orderId", "ORD-001"))
        );
    }

    private FluxyExecutionEntity createExecutionEntity(FluxyFlowEntity flow, ExecutionStatus status) {
        FluxyExecutionEntity exec = new FluxyExecutionEntity();
        exec.setId(UUID.randomUUID());
        exec.setFlow(flow);
        exec.setStatus(status);

        ExecutionContextEntity ctx = new ExecutionContextEntity();
        ctx.setType(flow.getName());
        ctx.setVersion("1.0");
        exec.setContext(ctx);

        return exec;
    }

    private FluxyStepEntity createStepEntity(String name) {
        FluxyStepEntity step = new FluxyStepEntity();
        step.setId(UUID.randomUUID());
        step.setName(name);
        return step;
    }

    private FlowStepEntity createFlowStepEntity(FluxyFlowEntity flow, FluxyStepEntity step, int order) {
        FlowStepEntity fs = new FlowStepEntity();
        fs.setId(UUID.randomUUID());
        fs.setFlow(flow);
        fs.setStep(step);
        fs.setStepOrder(order);
        return fs;
    }

    private StepTaskEntity createStepTaskEntity(FluxyStepEntity step, String taskName, int order) {
        FluxyTaskEntity taskEntity = new FluxyTaskEntity();
        taskEntity.setId(UUID.randomUUID());
        taskEntity.setName(taskName);
        taskEntity.setVersion(1);

        StepTaskEntity st = new StepTaskEntity();
        st.setId(UUID.randomUUID());
        st.setStep(step);
        st.setTask(taskEntity);
        st.setTaskOrder(order);
        return st;
    }

    private FluxyTask createFluxyTask(String name, int version) {
        return new FluxyTask() {
            {
                this.name = name;
                this.version = version;
            }
            @Override
            public TaskResult execute(ExecutionContext executionContext) {
                return TaskResult.SUCCESS;
            }
        };
    }

    // ── initializeFlow ──────────────────────────────────────────────────────

    @Nested
    class InitializeFlowTests {

        @Test
        void initializeFlow_createsExecution() {
            FluxyFlowEntity flow = createFlowEntity("my-flow");
            ExecutionContextRequest request = createRequest("my-flow");

            when(flowRepository.findById(flow.getId())).thenReturn(Optional.of(flow));
            when(executionRepository.findByFlowAndIdempotencyKeyAndStatusNot(eq(flow), any(), eq(ExecutionStatus.FINISHED)))
                    .thenReturn(Optional.empty());
            when(flowStepRepository.findByFlowOrderByStepOrderAsc(flow)).thenReturn(List.of());
            when(executionRepository.save(any())).thenAnswer(inv -> {
                FluxyExecutionEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            FlowExecutionResultDto result = service.initializeFlow(flow.getId(), request);

            assertNotNull(result);
            assertEquals("my-flow", result.flowName());
            assertEquals("PENDING", result.executionStatus());
            verify(executionRepository).save(any());
        }

        @Test
        void initializeFlow_idempotent_returnsExisting() {
            FluxyFlowEntity flow = createFlowEntity("my-flow");
            ExecutionContextRequest request = createRequest("my-flow");
            FluxyExecutionEntity existing = createExecutionEntity(flow, ExecutionStatus.RUNNING);

            when(flowRepository.findById(flow.getId())).thenReturn(Optional.of(flow));
            when(executionRepository.findByFlowAndIdempotencyKeyAndStatusNot(eq(flow), any(), eq(ExecutionStatus.FINISHED)))
                    .thenReturn(Optional.of(existing));
            when(stepRecordRepository.findByExecutionOrderByFlowStepStepOrderAsc(existing))
                    .thenReturn(List.of());

            FlowExecutionResultDto result = service.initializeFlow(flow.getId(), request);

            assertEquals("RUNNING", result.executionStatus());
            verify(executionRepository, never()).save(any());
        }

        @Test
        void initializeFlow_throwsWhenNoReferences() {
            ExecutionContextRequest request = new ExecutionContextRequest("type", "1.0", null, null);

            assertThrows(IllegalArgumentException.class,
                    () -> service.initializeFlow(UUID.randomUUID(), request));
        }

        @Test
        void initializeFlow_throwsWhenEmptyReferences() {
            ExecutionContextRequest request = new ExecutionContextRequest("type", "1.0", null, List.of());

            assertThrows(IllegalArgumentException.class,
                    () -> service.initializeFlow(UUID.randomUUID(), request));
        }

        @Test
        void initializeFlow_throwsWhenTypeDoesNotMatchFlowName() {
            FluxyFlowEntity flow = createFlowEntity("my-flow");
            ExecutionContextRequest request = createRequest("wrong-type");

            when(flowRepository.findById(flow.getId())).thenReturn(Optional.of(flow));

            assertThrows(IllegalArgumentException.class,
                    () -> service.initializeFlow(flow.getId(), request));
        }

        @Test
        void initializeFlow_throwsWhenFlowNotFound() {
            UUID id = UUID.randomUUID();
            ExecutionContextRequest request = createRequest("my-flow");

            when(flowRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.initializeFlow(id, request));
        }

        @Test
        void initializeFlow_throwsOnDuplicateReferenceTypes() {
            FluxyFlowEntity flow = createFlowEntity("my-flow");
            ExecutionContextRequest request = new ExecutionContextRequest(
                    "my-flow", "1.0", null,
                    List.of(new ReferenceDto("orderId", "1"), new ReferenceDto("orderId", "2"))
            );

            when(flowRepository.findById(flow.getId())).thenReturn(Optional.of(flow));
            when(executionRepository.findByFlowAndIdempotencyKeyAndStatusNot(eq(flow), any(), eq(ExecutionStatus.FINISHED)))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.initializeFlow(flow.getId(), request));
        }
    }

    // ── initializeFlowByName ────────────────────────────────────────────────

    @Test
    void initializeFlowByName_delegatesToInitializeFlow() {
        FluxyFlowEntity flow = createFlowEntity("my-flow");
        ExecutionContextRequest request = createRequest("my-flow");

        when(flowRepository.findByName("my-flow")).thenReturn(Optional.of(flow));
        when(flowRepository.findById(flow.getId())).thenReturn(Optional.of(flow));
        when(executionRepository.findByFlowAndIdempotencyKeyAndStatusNot(eq(flow), any(), eq(ExecutionStatus.FINISHED)))
                .thenReturn(Optional.empty());
        when(flowStepRepository.findByFlowOrderByStepOrderAsc(flow)).thenReturn(List.of());
        when(executionRepository.save(any())).thenAnswer(inv -> {
            FluxyExecutionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        FlowExecutionResultDto result = service.initializeFlowByName("my-flow", request);

        assertNotNull(result);
        assertEquals("my-flow", result.flowName());
    }

    @Test
    void initializeFlowByName_throwsWhenNameNotFound() {
        when(flowRepository.findByName("nope")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.initializeFlowByName("nope", createRequest("nope")));
    }

    // ── processExecution ────────────────────────────────────────────────────

    @Nested
    class ProcessExecutionTests {

        @Test
        void processExecution_throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(executionRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.processExecution(id, null));
        }

        @Test
        void processExecution_throwsWhenNoStepsPending() {
            FluxyFlowEntity flow = createFlowEntity("my-flow");
            FluxyExecutionEntity exec = createExecutionEntity(flow, ExecutionStatus.RUNNING);

            ExecutionStepRecordEntity finishedStep = new ExecutionStepRecordEntity();
            finishedStep.setStepStatus(StepStatus.FINISHED);
            FluxyStepEntity step = createStepEntity("step-1");
            FlowStepEntity fs = createFlowStepEntity(flow, step, 1);
            finishedStep.setFlowStep(fs);

            when(executionRepository.findById(exec.getId())).thenReturn(Optional.of(exec));
            when(stepRecordRepository.findByExecutionOrderByFlowStepStepOrderAsc(exec))
                    .thenReturn(List.of(finishedStep));

            assertThrows(IllegalStateException.class,
                    () -> service.processExecution(exec.getId(), null));
        }
    }

    // ── getExecution ────────────────────────────────────────────────────────

    @Test
    void getExecution_returnsResult() {
        FluxyFlowEntity flow = createFlowEntity("my-flow");
        FluxyExecutionEntity exec = createExecutionEntity(flow, ExecutionStatus.PENDING);

        when(executionRepository.findById(exec.getId())).thenReturn(Optional.of(exec));
        when(stepRecordRepository.findByExecutionOrderByFlowStepStepOrderAsc(exec))
                .thenReturn(List.of());

        FlowExecutionResultDto result = service.getExecution(exec.getId());

        assertNotNull(result);
        assertEquals("PENDING", result.executionStatus());
        assertEquals("my-flow", result.flowName());
    }

    @Test
    void getExecution_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(executionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getExecution(id));
    }

    // ── executeTask ─────────────────────────────────────────────────────────

    @Nested
    class ExecuteTaskTests {

        @Test
        void executeTask_latestVersion() {
            FluxyTask task = createFluxyTask("send-email", 2);
            when(taskRegistry.findLatestByName("send-email")).thenReturn(Optional.of(task));
            when(taskExecutorService.executeTask(eq(task), any())).thenReturn(TaskResult.SUCCESS);

            ExecutionContextRequest request = new ExecutionContextRequest("default", "1.0", null, null);
            TaskExecutionResultDto result = service.executeTask("send-email", request);

            assertEquals("send-email", result.taskName());
            assertEquals("FINISHED", result.status());
            assertEquals("SUCCESS", result.result());
        }

        @Test
        void executeTask_specificVersion() {
            FluxyTask task = createFluxyTask("send-email", 1);
            when(taskRegistry.findByNameAndVersion("send-email", 1)).thenReturn(Optional.of(task));
            when(taskExecutorService.executeTask(eq(task), any())).thenReturn(TaskResult.FAILURE);

            ExecutionContextRequest request = new ExecutionContextRequest("default", "1.0", null, null);
            TaskExecutionResultDto result = service.executeTask("send-email", 1, request);

            assertEquals("send-email", result.taskName());
            assertEquals("FAILURE", result.result());
        }

        @Test
        void executeTask_throwsWhenNotFound() {
            when(taskRegistry.findLatestByName("nope")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.executeTask("nope", null));
        }

        @Test
        void executeTask_versionedThrowsWhenNotFound() {
            when(taskRegistry.findByNameAndVersion("nope", 1)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.executeTask("nope", 1, null));
        }
    }

    // ── processStep ─────────────────────────────────────────────────────────

    @Nested
    class ProcessStepTests {

        @Test
        void processStep_executesFirstTask() {
            FluxyStepEntity step = createStepEntity("step-1");
            StepTaskEntity stepTask = createStepTaskEntity(step, "send-email", 1);
            FluxyTask fluxyTask = createFluxyTask("send-email", 1);

            when(stepRepository.findById(step.getId())).thenReturn(Optional.of(step));
            when(stepTaskRepository.findByStepOrderByTaskOrderAsc(step)).thenReturn(List.of(stepTask));
            when(taskRegistry.findByNameAndVersion("send-email", 1)).thenReturn(Optional.of(fluxyTask));
            when(taskExecutorService.executeTask(eq(fluxyTask), any())).thenReturn(TaskResult.SUCCESS);

            StepExecutionResultDto result = service.processStep(step.getId(), createRequest("default"));

            assertEquals("step-1", result.stepName());
            assertEquals(1, result.tasks().size());
            verify(taskExecutorService).executeTask(eq(fluxyTask), any());
        }

        @Test
        void processStep_throwsWhenStepNotFound() {
            UUID id = UUID.randomUUID();
            when(stepRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.processStep(id, createRequest("default")));
        }

        @Test
        void processStepByName_delegatesToProcessStep() {
            FluxyStepEntity step = createStepEntity("step-1");
            StepTaskEntity stepTask = createStepTaskEntity(step, "send-email", 1);
            FluxyTask fluxyTask = createFluxyTask("send-email", 1);

            when(stepRepository.findByName("step-1")).thenReturn(Optional.of(step));
            when(stepRepository.findById(step.getId())).thenReturn(Optional.of(step));
            when(stepTaskRepository.findByStepOrderByTaskOrderAsc(step)).thenReturn(List.of(stepTask));
            when(taskRegistry.findByNameAndVersion("send-email", 1)).thenReturn(Optional.of(fluxyTask));
            when(taskExecutorService.executeTask(eq(fluxyTask), any())).thenReturn(TaskResult.SUCCESS);

            StepExecutionResultDto result = service.processStepByName("step-1", createRequest("default"));

            assertEquals("step-1", result.stepName());
        }
    }

    // ── Deprecated methods removed ──────────────────────────────────────────

    @Test
    void processFlow_methodDoesNotExist() {
        for (var method : FluxyExecutionService.class.getDeclaredMethods()) {
            assertNotEquals("processFlow", method.getName(),
                    "Deprecated method 'processFlow' should have been removed");
            assertNotEquals("processFlowByName", method.getName(),
                    "Deprecated method 'processFlowByName' should have been removed");
        }
    }
}

