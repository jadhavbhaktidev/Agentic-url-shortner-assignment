package com.agentic.orchestration.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class WorkflowState {
    private String runId;
    private String projectState;
    private String workflowStatus;
    private String dagVersion;
    private Instant createdAt;
    private Instant lastModifiedAt;
    private Map<String, WorkflowNode> nodes;
    private List<Approval> approvals;
    private List<Transition> transitions;
    private List<String> risks;
    private List<Retry> retries;
    private List<Rollback> rollbacks;

    public static class Approval {
        public String id;
        public String gate;
        public String status;
        public String requiredRole;
        public String approvedBy;
        public Instant approvedAt;

        public Approval(String id, String gate, String status, String requiredRole) {
            this.id = id;
            this.gate = gate;
            this.status = status;
            this.requiredRole = requiredRole;
        }
    }

    public static class Transition {
        public Instant at;
        public String from;
        public String to;
        public String node;
        public String actor;
        public String reason;
    }

    public static class Retry {
        public String nodeId;
        public int count;
        public String reason;
        public Instant at;
    }

    public static class Rollback {
        public String nodeId;
        public String scope;
        public String reason;
        public Instant at;
    }

    // Constructors and getters
    public WorkflowState(String runId, String dagVersion) {
        this.runId = runId;
        this.dagVersion = dagVersion;
        this.createdAt = Instant.now();
        this.projectState = "ACTIVE";
        this.workflowStatus = "READY_FOR_EXECUTION";
    }

    public String getRunId() { return runId; }
    public String getProjectState() { return projectState; }
    public void setProjectState(String projectState) { this.projectState = projectState; }
    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
    public Map<String, WorkflowNode> getNodes() { return nodes; }
    public void setNodes(Map<String, WorkflowNode> nodes) { this.nodes = nodes; }
    public List<Approval> getApprovals() { return approvals; }
    public void setApprovals(List<Approval> approvals) { this.approvals = approvals; }
    public List<Transition> getTransitions() { return transitions; }
    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Instant lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public List<Retry> getRetries() { return retries; }
    public void setRetries(List<Retry> retries) { this.retries = retries; }
    public List<Rollback> getRollbacks() { return rollbacks; }
    public void setRollbacks(List<Rollback> rollbacks) { this.rollbacks = rollbacks; }
}
