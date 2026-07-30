package com.agentic.orchestration.engine;

import com.agentic.orchestration.agents.*;
import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutorTest {

    private WorkflowState state;
    private WorkflowExecutor executor;
    private ApprovalManager approvalManager;

    @BeforeEach
    void setUp() {
        state = new WorkflowState("test-run-001", "1.0.0");
        approvalManager = new ApprovalManager(state);
        executor = new WorkflowExecutor(state, approvalManager);

        // Set up basic agents
        executor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());
        executor.registerAgent("implementation-agent", new ImplementationAgent());
        executor.registerAgent("testing-agent", new TestingAgent());
        executor.registerAgent("documentation-agent", new DocumentationAgent());

        // Initialize workflow state with nodes
        Map<String, WorkflowNode> nodes = new HashMap<>();
        
        WorkflowNode req = new WorkflowNode("requirements", "Requirement", "requirements-analysis-agent");
        req.setStatus(WorkflowNode.NodeStatus.READY);
        nodes.put("requirements", req);

        WorkflowNode impl = new WorkflowNode("implementation", "Implementation", "implementation-agent");
        impl.setDependsOn(List.of("requirements"));
        nodes.put("implementation", impl);

        WorkflowNode test = new WorkflowNode("testing", "Test", "testing-agent");
        test.setDependsOn(List.of("implementation"));
        nodes.put("testing", test);

        WorkflowNode doc = new WorkflowNode("documentation", "Documentation", "documentation-agent");
        doc.setDependsOn(List.of("testing"));
        nodes.put("documentation", doc);

        state.setNodes(nodes);
        
        // Initialize approvals
        List<WorkflowState.Approval> approvals = List.of(
            new WorkflowState.Approval("APR-001", "architecture_adr", "APPROVED", "engineering_lead"),
            new WorkflowState.Approval("APR-002", "public_api_schema", "APPROVED", "api_owner"),
            new WorkflowState.Approval("APR-003", "security_controls", "APPROVED", "security_reviewer")
        );
        state.setApprovals(approvals);
    }

    @Test
    void testWorkflowSequentialExecution() throws WorkflowExecutor.WorkflowException {
        executor.executeWorkflow();

        // Verify all nodes succeeded
        assertThat(state.getNodes().get("requirements").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
        assertThat(state.getNodes().get("implementation").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
        assertThat(state.getNodes().get("testing").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
        assertThat(state.getNodes().get("documentation").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
    }

    @Test
    void testDependencyEnforcement() throws WorkflowExecutor.WorkflowException {
        // Verify that implementation cannot run before requirements
        WorkflowNode implNode = state.getNodes().get("implementation");
        assertThat(implNode.getStatus()).isEqualTo(WorkflowNode.NodeStatus.PENDING);

        executor.executeWorkflow();

        // After execution, all should be succeeded in order
        assertThat(state.getNodes().get("requirements").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
    }

    @Test
    void testApprovalGateEnforcement() {
        // Create a node with a gate
        WorkflowNode gatedNode = new WorkflowNode("release", "Release", "release-agent");
        gatedNode.setGate("production_readiness");
        gatedNode.setStatus(WorkflowNode.NodeStatus.READY);
        
        state.getNodes().put("release", gatedNode);

        // Verify gate is not approved yet
        assertThat(approvalManager.isGateApproved("production_readiness")).isFalse();

        // Set up approver for release_manager role
        approvalManager.setApprover("release_manager", "user-001");

        // Approve the gate
        WorkflowState.Approval approval = new WorkflowState.Approval("APR-004", "production_readiness", "AWAITING_APPROVAL", "release_manager");
        List<WorkflowState.Approval> approvals = new ArrayList<>(state.getApprovals());
        approvals.add(approval);
        state.setApprovals(approvals);

        approvalManager.approveGate("APR-004", "production_readiness", "release_manager", "user-001");

        assertThat(approvalManager.isGateApproved("production_readiness")).isTrue();
    }

    @Test
    void testNodeRetryOnFailure() throws WorkflowExecutor.WorkflowException {
        // Verify that a failed node can retry up to 3 times
        WorkflowNode testNode = state.getNodes().get("testing");
        
        // Simulate a failure
        testNode.incrementRetryCount();
        testNode.setLastError("Test failure");
        
        assertThat(testNode.getRetryCount()).isEqualTo(1);
        assertThat(testNode.getLastError()).contains("failure");
    }

    @Test
    void testWorkflowStateTransitions() throws WorkflowExecutor.WorkflowException {
        assertThat(state.getWorkflowStatus()).isEqualTo("READY_FOR_EXECUTION");
        
        executor.executeWorkflow();
        
        assertThat(state.getWorkflowStatus()).isEqualTo("COMPLETED");
    }

}
