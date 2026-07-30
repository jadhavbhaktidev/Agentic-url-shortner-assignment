package com.agentic.orchestration;

import com.agentic.orchestration.engine.WorkflowExecutor;
import com.agentic.orchestration.engine.ApprovalManager;
import com.agentic.orchestration.agents.*;
import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class OrchestratorMain {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorMain.class);

    public static void main(String[] args) throws Exception {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  Agentic URL Shortener Orchestrator - Workflow Execution        ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("");

        // 1. Initialize workflow state
        String runId = "wf-" + System.currentTimeMillis();
        WorkflowState state = new WorkflowState(runId, "1.0.0");
        logger.info("✓ WorkflowState initialized: {}", runId);

        // 2. Initialize approval manager and set approvers
        ApprovalManager approvalManager = new ApprovalManager(state);
        approvalManager.setApprover("product_owner", "alice@company.com");
        approvalManager.setApprover("engineering_lead", "bob@company.com");
        approvalManager.setApprover("api_owner", "charlie@company.com");
        approvalManager.setApprover("security_reviewer", "diana@company.com");
        approvalManager.setApprover("release_manager", "eve@company.com");
        logger.info("✓ Approvers configured (5 roles)");

        // 3. Initialize orchestration executor
        WorkflowExecutor executor = new WorkflowExecutor(state, approvalManager);
        logger.info("✓ Orchestration executor initialized");

        // 4. Register agents
        executor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());
        executor.registerAgent("implementation-agent", new ImplementationAgent());
        executor.registerAgent("testing-agent", new TestingAgent());
        executor.registerAgent("documentation-agent", new DocumentationAgent());
        logger.info("✓ 4 agents registered:");
        logger.info("  - RequirementsAnalysisAgent");
        logger.info("  - ImplementationAgent");
        logger.info("  - TestingAgent");
        logger.info("  - DocumentationAgent");

        // 5. Build workflow nodes (18-node DAG, simplified to 4 for demo)
        Map<String, WorkflowNode> nodes = buildWorkflowDAG();
        state.setNodes(nodes);
        logger.info("✓ Workflow DAG built with {} nodes", nodes.size());

        // 6. Initialize approvals (all pre-approved for demo)
        List<WorkflowState.Approval> approvals = initializeApprovals();
        state.setApprovals(approvals);
        logger.info("✓ {} approval gates initialized and approved", approvals.size());

        logger.info("");
        logger.info("Starting workflow execution...");
        logger.info("");

        // 7. Execute workflow
        long startTime = System.currentTimeMillis();
        try {
            executor.executeWorkflow();
        } catch (WorkflowExecutor.WorkflowException e) {
            logger.error("Workflow execution failed: {}", e.getMessage(), e);
            System.exit(1);
        }
        long duration = System.currentTimeMillis() - startTime;

        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║                    EXECUTION RESULTS                            ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("");

        // 8. Print results
        printResults(state, duration);
    }

    private static Map<String, WorkflowNode> buildWorkflowDAG() {
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();

        // Tier 1: Requirements Analysis
        WorkflowNode requirements = new WorkflowNode("requirements", "Requirement", "requirements-analysis-agent");
        requirements.setStatus(WorkflowNode.NodeStatus.READY);
        requirements.setExit("normalized_requirements");
        nodes.put("requirements", requirements);

        // Tier 2: Implementation
        WorkflowNode implementation = new WorkflowNode("implementation", "Implementation", "implementation-agent");
        implementation.setDependsOn(List.of("requirements"));
        nodes.put("implementation", implementation);

        // Tier 3: Testing
        WorkflowNode testing = new WorkflowNode("testing", "Test", "testing-agent");
        testing.setDependsOn(List.of("implementation"));
        nodes.put("testing", testing);

        // Tier 4: Documentation & Release
        WorkflowNode documentation = new WorkflowNode("documentation", "Documentation", "documentation-agent");
        documentation.setDependsOn(List.of("testing"));
        nodes.put("documentation", documentation);

        return nodes;
    }

    private static List<WorkflowState.Approval> initializeApprovals() {
        return Arrays.asList(
            new WorkflowState.Approval("APR-001", "requirement_interpretation", "APPROVED", "product_owner"),
            new WorkflowState.Approval("APR-002", "architecture_adr", "APPROVED", "engineering_lead"),
            new WorkflowState.Approval("APR-003", "public_api_schema", "APPROVED", "api_owner"),
            new WorkflowState.Approval("APR-004", "security_controls", "APPROVED", "security_reviewer"),
            new WorkflowState.Approval("APR-005", "production_readiness", "APPROVED", "release_manager")
        );
    }

    private static void printResults(WorkflowState state, long duration) {
        Logger logger = LoggerFactory.getLogger(OrchestratorMain.class);

        // Summary
        logger.info("Run ID:        {}", state.getRunId());
        logger.info("Status:        {}", state.getWorkflowStatus());
        logger.info("Duration:      {} ms", duration);
        logger.info("");

        // Node execution details
        logger.info("Node Execution Results:");
        logger.info("");

        int successCount = 0;
        int failureCount = 0;

        for (WorkflowNode node : state.getNodes().values()) {
            String status = node.getStatus().toString();
            String icon = "○";

            if (node.getStatus() == WorkflowNode.NodeStatus.SUCCEEDED) {
                icon = "✓";
                successCount++;
            } else if (node.getStatus() == WorkflowNode.NodeStatus.FAILED) {
                icon = "✗";
                failureCount++;
            } else if (node.getStatus() == WorkflowNode.NodeStatus.AWAITING_APPROVAL) {
                icon = "⊙";
            }

            logger.info("  {} {:<20} [{}]", icon, node.getId(), status);

            if (node.getStartedAt() != null && node.getCompletedAt() != null) {
                long nodeDuration = node.getCompletedAt().toEpochMilli() - node.getStartedAt().toEpochMilli();
                logger.info("     Started:  {}", node.getStartedAt());
                logger.info("     Duration: {} ms", nodeDuration);
            }

            if (node.getOutput() != null && !node.getOutput().isEmpty()) {
                logger.info("     Output:");
                node.getOutput().forEach((key, value) -> {
                    logger.info("       - {}: {}", key, value);
                });
            }

            if (node.getLastError() != null) {
                logger.info("     Error: {}", node.getLastError());
            }

            logger.info("");
        }

        // Summary statistics
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Total Nodes:    {}", state.getNodes().size());
        logger.info("Succeeded:      {} ✓", successCount);
        logger.info("Failed:         {} ✗", failureCount);
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("");

        // Approval gates summary
        logger.info("Approval Gates:");
        for (WorkflowState.Approval approval : state.getApprovals()) {
            String icon = "APPROVED".equalsIgnoreCase(approval.status) ? "✓" : "○";
            logger.info("  {} {:<30} [{}] by {}", 
                icon, 
                approval.gate, 
                approval.status, 
                approval.approvedBy != null ? approval.approvedBy : "pending");
        }

        logger.info("");
        if (successCount == state.getNodes().size() && failureCount == 0) {
            logger.info("╔════════════════════════════════════════════════════════════════╗");
            logger.info("║                   ✓ WORKFLOW COMPLETED SUCCESSFULLY             ║");
            logger.info("╚════════════════════════════════════════════════════════════════╝");
        } else {
            logger.info("╔════════════════════════════════════════════════════════════════╗");
            logger.info("║                    ✗ WORKFLOW COMPLETED WITH ISSUES             ║");
            logger.info("╚════════════════════════════════════════════════════════════════╝");
        }

        logger.info("");
        logger.info("Next Steps:");
        logger.info("  1. Review node outputs above");
        logger.info("  2. Check approval gates status");
        logger.info("  3. If all gates approved, proceed to release");
        logger.info("  4. For details, see execution/reports/final-engineering-summary.md");
        logger.info("");
    }
}
