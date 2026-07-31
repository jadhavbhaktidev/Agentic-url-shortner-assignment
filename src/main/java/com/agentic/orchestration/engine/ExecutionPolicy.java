package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ExecutionPolicy {
    public enum ExecutionMode {
        EXECUTE,
        PLAN_ONLY
    }

    public record Decision(boolean allowed, WorkflowNode.NodeStatus blockedStatus, String reasonCode, String reason) {
        public static Decision allow() {
            return new Decision(true, null, "ALLOWED", "Execution permitted by policy");
        }

        public static Decision block(WorkflowNode.NodeStatus blockedStatus, String reasonCode, String reason) {
            return new Decision(false, blockedStatus, reasonCode, reason);
        }
    }

    private final ExecutionMode mode;
    private final Set<String> approvalRequiredNodeTypes;
    private final Set<String> approvalRequiredOwners;
    private final boolean failClosedOnMissingGate;

    public ExecutionPolicy(
        ExecutionMode mode,
        Set<String> approvalRequiredNodeTypes,
        Set<String> approvalRequiredOwners,
        boolean failClosedOnMissingGate
    ) {
        this.mode = mode;
        this.approvalRequiredNodeTypes = normalize(approvalRequiredNodeTypes);
        this.approvalRequiredOwners = normalize(approvalRequiredOwners);
        this.failClosedOnMissingGate = failClosedOnMissingGate;
    }

    public static ExecutionPolicy defaultPolicy() {
        return new ExecutionPolicy(
            ExecutionMode.EXECUTE,
            Collections.emptySet(),
            Collections.emptySet(),
            true
        );
    }

    public static ExecutionPolicy planOnlyPolicy() {
        ExecutionPolicy defaultPolicy = defaultPolicy();
        return new ExecutionPolicy(
            ExecutionMode.PLAN_ONLY,
            defaultPolicy.approvalRequiredNodeTypes,
            defaultPolicy.approvalRequiredOwners,
            defaultPolicy.failClosedOnMissingGate
        );
    }

    public Decision evaluate(WorkflowNode node, ApprovalManager approvalManager) {
        if (mode == ExecutionMode.PLAN_ONLY) {
            return Decision.block(
                WorkflowNode.NodeStatus.AWAITING_APPROVAL,
                "PLAN_ONLY_MODE",
                "Execution disabled in PLAN_ONLY mode; human approval required to execute"
            );
        }

        if (!isApprovalRequired(node)) {
            return Decision.allow();
        }

        String gate = node.getGate();
        if ((gate == null || gate.isBlank()) && failClosedOnMissingGate) {
            return Decision.block(
                WorkflowNode.NodeStatus.BLOCKED,
                "MISSING_RISK_GATE",
                "Policy requires an approval gate for risky node " + node.getId()
            );
        }

        if (gate != null && !gate.isBlank() && !approvalManager.isGateApproved(gate)) {
            return Decision.block(
                WorkflowNode.NodeStatus.AWAITING_APPROVAL,
                "RISK_GATE_NOT_APPROVED",
                "Policy gate not approved: " + gate
            );
        }

        return Decision.allow();
    }

    private boolean isApprovalRequired(WorkflowNode node) {
        return approvalRequiredNodeTypes.contains(normalize(node.getType()))
            || approvalRequiredOwners.contains(normalize(node.getOwner()));
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(normalize(value));
            }
        }
        return normalized;
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
    }
}