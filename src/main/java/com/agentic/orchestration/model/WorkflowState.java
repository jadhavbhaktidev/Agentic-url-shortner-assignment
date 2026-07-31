package com.agentic.orchestration.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public WorkflowState() {
        this.nodes = new LinkedHashMap<>();
        this.approvals = new ArrayList<>();
        this.transitions = new ArrayList<>();
        this.risks = new ArrayList<>();
        this.retries = new ArrayList<>();
        this.rollbacks = new ArrayList<>();
        this.createdAt = Instant.now();
        this.projectState = "ACTIVE";
        this.workflowStatus = "READY_FOR_EXECUTION";
    }

    public static class Approval {
        public String id;
        public String gate;
        public String status;
        public String requiredRole;
        public String approvedBy;
        public Instant approvedAt;

        public Approval() {
        }

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

        public Transition() {
        }
    }

    public static class Retry {
        public String nodeId;
        public int count;
        public String reason;
        public Instant at;

        public Retry() {
        }
    }

    public static class Rollback {
        public String nodeId;
        public String scope;
        public String reason;
        public Instant at;

        public Rollback() {
        }
    }

    // Constructors and getters
    public WorkflowState(String runId, String dagVersion) {
        this();
        this.runId = runId;
        this.dagVersion = dagVersion;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getProjectState() { return projectState; }
    public void setProjectState(String projectState) { this.projectState = projectState; }
    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
    public String getDagVersion() { return dagVersion; }
    public void setDagVersion(String dagVersion) { this.dagVersion = dagVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Map<String, WorkflowNode> getNodes() { return nodes; }
    public void setNodes(Map<String, WorkflowNode> nodes) { this.nodes = nodes != null ? nodes : new LinkedHashMap<>(); }
    public List<Approval> getApprovals() { return approvals; }
    public void setApprovals(List<Approval> approvals) { this.approvals = approvals != null ? approvals : new ArrayList<>(); }
    public List<Transition> getTransitions() { return transitions; }
    public void setTransitions(List<Transition> transitions) { this.transitions = transitions != null ? transitions : new ArrayList<>(); }
    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks != null ? risks : new ArrayList<>(); }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Instant lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public List<Retry> getRetries() { return retries; }
    public void setRetries(List<Retry> retries) { this.retries = retries != null ? retries : new ArrayList<>(); }
    public List<Rollback> getRollbacks() { return rollbacks; }
    public void setRollbacks(List<Rollback> rollbacks) { this.rollbacks = rollbacks != null ? rollbacks : new ArrayList<>(); }

    public void recordTransition(String from, String to, String node, String actor, String reason) {
        Transition transition = new Transition();
        transition.at = Instant.now();
        transition.from = from;
        transition.to = to;
        transition.node = node;
        transition.actor = actor;
        transition.reason = reason;
        if (transitions == null) {
            transitions = new ArrayList<>();
        }
        transitions.add(transition);
        lastModifiedAt = transition.at;
    }

    public String toJson() {
        try {
            return objectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize workflow state", e);
        }
    }

    public static WorkflowState fromJson(String json) {
        try {
            return objectMapper().readValue(json, WorkflowState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize workflow state", e);
        }
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
