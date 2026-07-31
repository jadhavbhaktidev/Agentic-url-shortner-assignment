package com.agentic.orchestration.engine;

import com.agentic.orchestration.agents.*;
import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutorTest {

    @TempDir
    Path tempDir;

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
        executor.registerAgent("validation-agent", new ValidationAgent());
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

        WorkflowNode validation = new WorkflowNode("validation", "Validation", "validation-agent");
        validation.setDependsOn(List.of("implementation"));
        nodes.put("validation", validation);

        WorkflowNode test = new WorkflowNode("testing", "Test", "testing-agent");
        test.setDependsOn(List.of("validation"));
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
        assertThat(state.getNodes().get("validation").getStatus())
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
        assertThat(state.getNodes().get("validation").getStatus())
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

    @Test
    void testWorkflowStateCheckpointRoundTrip() throws WorkflowExecutor.WorkflowException {
        executor.executeWorkflow();

        String checkpoint = state.toJson();
        WorkflowState restored = WorkflowState.fromJson(checkpoint);

        assertThat(restored.getRunId()).isEqualTo(state.getRunId());
        assertThat(restored.getWorkflowStatus()).isEqualTo(state.getWorkflowStatus());
        assertThat(restored.getNodes()).containsKeys("requirements", "implementation", "validation", "testing", "documentation");
        assertThat(restored.getNodes().get("validation").getStatus()).isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
    }

    @Test
    void testWorkflowFailureBlocksDependentsAfterRetries() throws WorkflowExecutor.WorkflowException {
        WorkflowState failingState = new WorkflowState("failing-run", "1.0.0");
        failingState.setApprovals(new ArrayList<>(state.getApprovals()));

        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        WorkflowNode root = new WorkflowNode("root", "Root", "failing-agent");
        root.setStatus(WorkflowNode.NodeStatus.READY);
        nodes.put("root", root);

        WorkflowNode dependent = new WorkflowNode("dependent", "Dependent", "testing-agent");
        dependent.setDependsOn(List.of("root"));
        nodes.put("dependent", dependent);
        failingState.setNodes(nodes);

        WorkflowExecutor failingExecutor = new WorkflowExecutor(failingState, new ApprovalManager(failingState));
        failingExecutor.registerAgent("failing-agent", (node, workflowState) -> {
            throw new IllegalStateException("boom");
        });
        failingExecutor.registerAgent("testing-agent", new TestingAgent());

        failingExecutor.executeWorkflow();

        assertThat(failingState.getNodes().get("root").getStatus()).isEqualTo(WorkflowNode.NodeStatus.FAILED);
        assertThat(failingState.getNodes().get("root").getRetryCount()).isEqualTo(3);
        assertThat(failingState.getNodes().get("dependent").getStatus()).isEqualTo(WorkflowNode.NodeStatus.BLOCKED);
        assertThat(failingState.getWorkflowStatus()).isEqualTo("PAUSED");
    }

    @Test
    void testWorkflowMetricsAreEmitted() throws WorkflowExecutor.WorkflowException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowExecutor meteredExecutor = new WorkflowExecutor(state, approvalManager, registry);
        meteredExecutor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());
        meteredExecutor.registerAgent("implementation-agent", new ImplementationAgent());
        meteredExecutor.registerAgent("validation-agent", new ValidationAgent());
        meteredExecutor.registerAgent("testing-agent", new TestingAgent());
        meteredExecutor.registerAgent("documentation-agent", new DocumentationAgent());

        meteredExecutor.executeWorkflow();

        double executionCount = registry.find("workflow.node.executions").counters().stream().mapToDouble(Counter::count).sum();
        double nodeTransitionCount = registry.find("workflow.node.transitions").counters().stream().mapToDouble(Counter::count).sum();
        double workflowTransitionCount = registry.find("workflow.transitions").counters().stream().mapToDouble(Counter::count).sum();

        assertThat(executionCount).isGreaterThan(0.0);
        assertThat(nodeTransitionCount).isGreaterThan(0.0);
        assertThat(workflowTransitionCount).isGreaterThan(0.0);
    }

    @Test
    void testIndependentBranchesCanCompleteInOneRun() throws WorkflowExecutor.WorkflowException {
        WorkflowState branchState = new WorkflowState("branch-run", "1.0.0");
        branchState.setApprovals(new ArrayList<>(state.getApprovals()));

        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        WorkflowNode root = new WorkflowNode("root", "Requirement", "requirements-analysis-agent");
        root.setStatus(WorkflowNode.NodeStatus.READY);
        nodes.put("root", root);

        WorkflowNode branchA = new WorkflowNode("branch-a", "Implementation", "implementation-agent");
        branchA.setDependsOn(List.of("root"));
        nodes.put("branch-a", branchA);

        WorkflowNode branchB = new WorkflowNode("branch-b", "Validation", "validation-agent");
        branchB.setDependsOn(List.of("root"));
        nodes.put("branch-b", branchB);

        branchState.setNodes(nodes);

        WorkflowExecutor branchExecutor = new WorkflowExecutor(branchState, new ApprovalManager(branchState));
        branchExecutor.registerAgent("requirements-analysis-agent", (node, workflowState) -> Map.of("status", "SUCCEEDED"));
        branchExecutor.registerAgent("implementation-agent", (node, workflowState) -> Map.of("status", "SUCCEEDED"));
        branchExecutor.registerAgent("validation-agent", (node, workflowState) -> Map.of("status", "SUCCEEDED"));

        branchExecutor.executeWorkflow();

        assertThat(branchState.getNodes().get("root").getStatus()).isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
        assertThat(branchState.getNodes().get("branch-a").getStatus()).isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
        assertThat(branchState.getNodes().get("branch-b").getStatus()).isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
    }

    @Test
    void testWorkflowPersistsCheckpointDuringExecution() throws Exception {
        FileWorkflowStateStore store = new FileWorkflowStateStore(tempDir);
        WorkflowExecutor persistentExecutor = new WorkflowExecutor(state, approvalManager, null, store);
        persistentExecutor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());
        persistentExecutor.registerAgent("implementation-agent", new ImplementationAgent());
        persistentExecutor.registerAgent("validation-agent", new ValidationAgent());
        persistentExecutor.registerAgent("testing-agent", new TestingAgent());
        persistentExecutor.registerAgent("documentation-agent", new DocumentationAgent());

        persistentExecutor.executeWorkflow();

        Path checkpoint = tempDir.resolve(state.getRunId() + ".json");
        assertThat(Files.exists(checkpoint)).isTrue();

        WorkflowState restored = store.load(state.getRunId()).orElseThrow();
        assertThat(restored.getWorkflowStatus()).isEqualTo("COMPLETED");
        assertThat(restored.getTransitions()).isNotEmpty();
    }

    @Test
    void testPolicyPlanOnlyModeBlocksExecution() throws WorkflowExecutor.WorkflowException {
        WorkflowState planState = new WorkflowState("plan-run", "1.0.0");
        WorkflowNode node = new WorkflowNode("requirements", "Requirement", "requirements-analysis-agent");
        node.setStatus(WorkflowNode.NodeStatus.READY);
        planState.setNodes(new LinkedHashMap<>(Map.of("requirements", node)));
        planState.setApprovals(new ArrayList<>(state.getApprovals()));

        WorkflowExecutor planExecutor = new WorkflowExecutor(
            planState,
            new ApprovalManager(planState),
            null,
            null,
            ExecutionPolicy.planOnlyPolicy()
        );
        planExecutor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());

        planExecutor.executeWorkflow();

        assertThat(planState.getNodes().get("requirements").getStatus())
            .isEqualTo(WorkflowNode.NodeStatus.AWAITING_APPROVAL);
        assertThat(planState.getWorkflowStatus()).isEqualTo("PAUSED");
    }

    @Test
    void testPolicyBlocksRiskyNodeWithoutGate() throws WorkflowExecutor.WorkflowException {
        WorkflowState riskyState = new WorkflowState("risky-run", "1.0.0");
        WorkflowNode riskyNode = new WorkflowNode("implementation", "Implementation", "implementation-agent");
        riskyNode.setStatus(WorkflowNode.NodeStatus.READY);
        riskyState.setNodes(new LinkedHashMap<>(Map.of("implementation", riskyNode)));
        riskyState.setApprovals(new ArrayList<>(state.getApprovals()));

        ExecutionPolicy policy = new ExecutionPolicy(
            ExecutionPolicy.ExecutionMode.EXECUTE,
            Set.of("Implementation"),
            Set.of(),
            true
        );

        WorkflowExecutor riskyExecutor = new WorkflowExecutor(
            riskyState,
            new ApprovalManager(riskyState),
            null,
            null,
            policy
        );
        riskyExecutor.registerAgent("implementation-agent", new ImplementationAgent());

        riskyExecutor.executeWorkflow();

        assertThat(riskyState.getNodes().get("implementation").getStatus()).isEqualTo(WorkflowNode.NodeStatus.BLOCKED);
        assertThat(riskyState.getWorkflowStatus()).isEqualTo("PAUSED");
    }

}
