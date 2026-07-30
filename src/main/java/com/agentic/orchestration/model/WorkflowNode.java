package com.agentic.orchestration.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class WorkflowNode {
    private String id;
    private String type;
    private String owner;
    private NodeStatus status;
    private List<String> dependsOn;
    private String gate;
    private String exit;
    private Map<String, Object> output;
    private Instant startedAt;
    private Instant completedAt;
    private int retryCount;
    private String lastError;

    public enum NodeStatus {
        PENDING, READY, RUNNING, SUCCEEDED, FAILED, BLOCKED, AWAITING_APPROVAL, ROLLBACK
    }

    public WorkflowNode(String id, String type, String owner) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.status = NodeStatus.PENDING;
        this.dependsOn = List.of();
        this.retryCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getOwner() { return owner; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
    public String getExit() { return exit; }
    public void setExit(String exit) { this.exit = exit; }
    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
