package com.agentic.orchestration;

import com.agentic.orchestration.engine.WorkflowExecutor;
import com.agentic.orchestration.engine.ApprovalManager;
import com.agentic.orchestration.engine.ExecutionPolicy;
import com.agentic.orchestration.engine.FileWorkflowStateStore;
import com.agentic.orchestration.engine.WorkflowStateStore;
import com.agentic.orchestration.agents.*;
import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.*;

public class OrchestratorMain {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorMain.class);

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);

        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  Agentic URL Shortener Orchestrator - Workflow Execution        ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("");

        // 1. Initialize workflow state store and workflow definition
        WorkflowStateStore stateStore = new FileWorkflowStateStore(options.checkpointDir());
        WorkflowState state;
        if (options.resumeCheckpointPath() != null) {
            state = stateStore.load(options.resumeCheckpointPath())
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + options.resumeCheckpointPath()));
            logger.info("✓ WorkflowState resumed from checkpoint: {}", options.resumeCheckpointPath());
        } else {
            String runId = "wf-" + System.currentTimeMillis();
            state = new WorkflowDefinitionLoader().load(options.workflowDefinitionPath(), runId);
            logger.info("✓ WorkflowState loaded from DAG: {}", options.workflowDefinitionPath());
        }

        // 2. Initialize approval manager and set approvers
        ApprovalManager approvalManager = new ApprovalManager(state);
        approvalManager.setApprover("product_owner", "alice@company.com");
        approvalManager.setApprover("engineering_lead", "bob@company.com");
        approvalManager.setApprover("api_owner", "charlie@company.com");
        approvalManager.setApprover("security_reviewer", "diana@company.com");
        approvalManager.setApprover("release_manager", "eve@company.com");
        logger.info("✓ Approvers configured (5 roles)");

        // 3. Initialize orchestration executor
        ExecutionPolicy executionPolicy = buildExecutionPolicy(options);
        WorkflowExecutor executor = new WorkflowExecutor(state, approvalManager, null, stateStore, executionPolicy);
        logger.info("✓ Orchestration executor initialized");
        logger.info("✓ Execution policy mode: {}", options.executionMode());

        // 4. Register agents
        executor.registerAgent("requirements-analysis-agent", new RequirementsAnalysisAgent());
        executor.registerAgent("implementation-agent", new ImplementationAgent());
        executor.registerAgent("validation-agent", new ValidationAgent());
        executor.registerAgent("testing-agent", new TestingAgent());
        executor.registerAgent("documentation-agent", new DocumentationAgent());
        logger.info("✓ 5 agents registered:");
        logger.info("  - RequirementsAnalysisAgent");
        logger.info("  - ImplementationAgent");
        logger.info("  - ValidationAgent");
        logger.info("  - TestingAgent");
        logger.info("  - DocumentationAgent");

        // 5. Initialize approvals if absent in checkpoint/workflow definition
        if (state.getApprovals() == null || state.getApprovals().isEmpty()) {
            List<WorkflowState.Approval> approvals = initializeApprovals();
            state.setApprovals(approvals);
            logger.info("✓ {} approval gates initialized and approved", approvals.size());
        }
        logger.info("✓ Workflow DAG loaded with {} nodes", state.getNodes().size());

        logger.info("");
        logger.info("Starting workflow execution...");
        logger.info("");

        // 6. Execute workflow
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

        // 7. Print results
        printResults(state, duration);
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

    private static ExecutionPolicy buildExecutionPolicy(CliOptions options) {
        ExecutionPolicy.ExecutionMode mode = "PLAN_ONLY".equalsIgnoreCase(options.executionMode())
            ? ExecutionPolicy.ExecutionMode.PLAN_ONLY
            : ExecutionPolicy.ExecutionMode.EXECUTE;
        return new ExecutionPolicy(
            mode,
            options.approvalRequiredNodeTypes(),
            options.approvalRequiredOwners(),
            options.failClosedOnMissingGate()
        );
    }

    private record CliOptions(
        Path workflowDefinitionPath,
        Path checkpointDir,
        Path resumeCheckpointPath,
        String executionMode,
        Set<String> approvalRequiredNodeTypes,
        Set<String> approvalRequiredOwners,
        boolean failClosedOnMissingGate
    ) {
        private static CliOptions parse(String[] args) {
            Path workflowPath = Path.of("execution", "workflow", "execution-dag.yaml");
            Path checkpointDirectory = Path.of("execution", "state");
            Path resumePath = null;
            String executionMode = "EXECUTE";
            Set<String> approvalRequiredTypes = new LinkedHashSet<>(Set.of("Implementation", "Security", "Release", "Governance"));
            Set<String> approvalRequiredOwners = new LinkedHashSet<>(Set.of("implementation-agent", "security-agent", "release-readiness-agent"));
            boolean failClosed = true;

            for (String arg : args) {
                if (arg.startsWith("--dag=")) {
                    workflowPath = Path.of(arg.substring("--dag=".length()));
                } else if (arg.startsWith("--checkpoint-dir=")) {
                    checkpointDirectory = Path.of(arg.substring("--checkpoint-dir=".length()));
                } else if (arg.startsWith("--resume=")) {
                    resumePath = Path.of(arg.substring("--resume=".length()));
                } else if (arg.startsWith("--mode=")) {
                    executionMode = arg.substring("--mode=".length()).toUpperCase(Locale.ROOT);
                } else if (arg.startsWith("--approval-required-types=")) {
                    approvalRequiredTypes = parseCsv(arg.substring("--approval-required-types=".length()));
                } else if (arg.startsWith("--approval-required-owners=")) {
                    approvalRequiredOwners = parseCsv(arg.substring("--approval-required-owners=".length()));
                } else if (arg.equals("--fail-open-missing-gate")) {
                    failClosed = false;
                }
            }

            return new CliOptions(
                workflowPath,
                checkpointDirectory,
                resumePath,
                executionMode,
                approvalRequiredTypes,
                approvalRequiredOwners,
                failClosed
            );
        }

        private static Set<String> parseCsv(String raw) {
            if (raw == null || raw.isBlank()) {
                return Collections.emptySet();
            }
            return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
