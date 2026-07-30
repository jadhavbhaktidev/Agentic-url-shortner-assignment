package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.*;

public class ApprovalManager {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalManager.class);
    private final WorkflowState state;
    private final Map<String, String> approversByRole;

    public ApprovalManager(WorkflowState state) {
        this.state = state;
        this.approversByRole = new HashMap<>();
    }

    public void setApprover(String role, String userId) {
        approversByRole.put(role, userId);
    }

    public boolean isGateApproved(String gateName) {
        return state.getApprovals().stream()
            .anyMatch(a -> a.gate.equals(gateName) && "APPROVED".equalsIgnoreCase(a.status));
    }

    public void approveGate(String gateId, String gateName, String approverRole, String userId) {
        WorkflowState.Approval approval = state.getApprovals().stream()
            .filter(a -> a.id.equals(gateId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Gate not found: " + gateId));

        if (!userId.equals(approversByRole.get(approverRole))) {
            throw new SecurityException("User not authorized for role: " + approverRole);
        }

        approval.status = "APPROVED";
        approval.approvedBy = userId;
        approval.approvedAt = Instant.now();
        state.setLastModifiedAt(Instant.now());

        logger.info("Gate {} approved by {} at {}", gateName, userId, approval.approvedAt);
    }

    public List<WorkflowState.Approval> getPendingApprovals() {
        return state.getApprovals().stream()
            .filter(a -> "AWAITING_APPROVAL".equalsIgnoreCase(a.status))
            .toList();
    }
}
