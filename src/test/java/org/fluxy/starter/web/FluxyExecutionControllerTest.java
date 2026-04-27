package org.fluxy.starter.web;

import org.fluxy.starter.dto.*;
import org.fluxy.starter.service.FluxyExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para FluxyExecutionController.
 * Verifica que cada método delega correctamente al servicio y que
 * los métodos deprecados eliminados ya no existen.
 *
 * <p>Nota: spring-webmvc es compileOnly, por lo que no se puede
 * instanciar el controlador directamente en los tests. Se verifica
 * la existencia/ausencia de métodos por reflexión.</p>
 */
@ExtendWith(MockitoExtension.class)
class FluxyExecutionControllerTest {

    @Mock
    private FluxyExecutionService executionService;

    private FluxyExecutionController controller;

    private final UUID flowId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();
    private final ExecutionContextRequest request = new ExecutionContextRequest(
            "test-flow", "1.0",
            List.of(new VariableDto("key", "value")),
            List.of(new ReferenceDto("orderId", "ORD-001"))
    );
    private final FlowExecutionResultDto flowResult = new FlowExecutionResultDto(
            executionId, "PENDING", flowId, "test-flow", "BATCH", List.of()
    );

    @BeforeEach
    void setUp() {
        controller = new FluxyExecutionController(executionService);
    }

    // ── initializeFlow ──────────────────────────────────────────────────────

    @Test
    void initializeFlow_delegatesToService() {
        when(executionService.initializeFlow(flowId, request)).thenReturn(flowResult);

        ResponseEntity<FlowExecutionResultDto> response = controller.initializeFlow(flowId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(flowResult, response.getBody());
        verify(executionService).initializeFlow(flowId, request);
    }

    @Test
    void initializeFlowByName_delegatesToService() {
        when(executionService.initializeFlowByName("test-flow", request)).thenReturn(flowResult);

        ResponseEntity<FlowExecutionResultDto> response = controller.initializeFlowByName("test-flow", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(flowResult, response.getBody());
    }

    // ── processExecution ────────────────────────────────────────────────────

    @Test
    void processExecution_delegatesToService() {
        when(executionService.processExecution(executionId, request)).thenReturn(flowResult);

        ResponseEntity<FlowExecutionResultDto> response = controller.processExecution(executionId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(flowResult, response.getBody());
    }

    @Test
    void processExecution_acceptsNullRequest() {
        when(executionService.processExecution(executionId, null)).thenReturn(flowResult);

        ResponseEntity<FlowExecutionResultDto> response = controller.processExecution(executionId, null);

        assertEquals(200, response.getStatusCode().value());
    }

    // ── getExecution ────────────────────────────────────────────────────────

    @Test
    void getExecution_delegatesToService() {
        when(executionService.getExecution(executionId)).thenReturn(flowResult);

        ResponseEntity<FlowExecutionResultDto> response = controller.getExecution(executionId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(flowResult, response.getBody());
    }

    // ── processStep ─────────────────────────────────────────────────────────

    @Test
    void processStep_delegatesToService() {
        UUID stepId = UUID.randomUUID();
        StepExecutionResultDto stepResult = new StepExecutionResultDto(stepId, "my-step", List.of());
        when(executionService.processStep(stepId, request)).thenReturn(stepResult);

        ResponseEntity<StepExecutionResultDto> response = controller.processStep(stepId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(stepResult, response.getBody());
    }

    @Test
    void processStepByName_delegatesToService() {
        UUID stepId = UUID.randomUUID();
        StepExecutionResultDto stepResult = new StepExecutionResultDto(stepId, "my-step", List.of());
        when(executionService.processStepByName("my-step", request)).thenReturn(stepResult);

        ResponseEntity<StepExecutionResultDto> response = controller.processStepByName("my-step", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(stepResult, response.getBody());
    }

    // ── executeTask ─────────────────────────────────────────────────────────

    @Test
    void executeTask_delegatesToService() {
        TaskExecutionResultDto taskResult = new TaskExecutionResultDto("send-email", "FINISHED", "SUCCESS");
        when(executionService.executeTask("send-email", request)).thenReturn(taskResult);

        ResponseEntity<TaskExecutionResultDto> response = controller.executeTask("send-email", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(taskResult, response.getBody());
    }

    @Test
    void executeTaskVersioned_delegatesToService() {
        TaskExecutionResultDto taskResult = new TaskExecutionResultDto("send-email", "FINISHED", "SUCCESS");
        when(executionService.executeTask("send-email", 2, request)).thenReturn(taskResult);

        ResponseEntity<TaskExecutionResultDto> response = controller.executeTaskVersioned("send-email", 2, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(taskResult, response.getBody());
    }

    // ── Verificación de API pública ─────────────────────────────────────────

    @Test
    void controller_hasInitializeFlowMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("initializeFlow", UUID.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasInitializeFlowByNameMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("initializeFlowByName", String.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasProcessExecutionMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("processExecution", UUID.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasGetExecutionMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("getExecution", UUID.class));
    }

    @Test
    void controller_hasProcessStepMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("processStep", UUID.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasProcessStepByNameMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("processStepByName", String.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasExecuteTaskMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("executeTask", String.class, ExecutionContextRequest.class));
    }

    @Test
    void controller_hasExecuteTaskVersionedMethod() {
        assertDoesNotThrow(() ->
                FluxyExecutionController.class.getMethod("executeTaskVersioned", String.class, int.class, ExecutionContextRequest.class));
    }

    // ── Deprecated methods removed ──────────────────────────────────────────

    @Test
    void processFlow_methodDoesNotExist() {
        for (Method method : FluxyExecutionController.class.getDeclaredMethods()) {
            assertNotEquals("processFlow", method.getName(),
                    "Deprecated method 'processFlow' should have been removed");
        }
    }

    @Test
    void processFlowByName_methodDoesNotExist() {
        for (Method method : FluxyExecutionController.class.getDeclaredMethods()) {
            assertNotEquals("processFlowByName", method.getName(),
                    "Deprecated method 'processFlowByName' should have been removed");
        }
    }
}

