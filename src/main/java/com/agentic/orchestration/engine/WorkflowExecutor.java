package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);
    private static final int MAX_RETRIES = 3;
    private final WorkflowState state;
    private final Map<String, Agent> agents;
    private final ApprovalManager approvalManager;

    public interface Agent {
        Map<String, Object> execute(WorkflowNode node, WorkflowState state);
    }

    public WorkflowExecutor(WorkflowState state, ApprovalManager approvalManager) {
        this.state = state;
        this.agents = new HashMap<>();
        this.approvalManager = approvalManager;
    }

    public void registerAgent(String owner, Agent agent) {
        agents.put(owner, agent);
    }

    public void executeWorkflow() throws WorkflowException {
        state.setWorkflowStatus("RUNNING");
        state.setLastModifiedAt(Instant.now());

        boolean workCompleted = false;
        while (!workCompleted) {
            WorkflowNode nextNode = findReadyNode();
            if (nextNode == null) {
                // Check if there are any blocked nodes or if we're done
                if (hasBlockedNodes()) {
                    logger.info("Workflow paused: nodes awaiting approval or dependencies");
                    workCompleted = true;
                } else if (allNodesTerminal()) {
                    logger.info("Workflow completed: all nodes terminal");
                    workCompleted = true;
                } else {
                    logger.warn("Workflow stuck: no ready nodes and unresolved blocks");
                    workCompleted = true;
                }
            } else {
                executeNode(nextNode);
            }
        }

        state.setWorkflowStatus("COMPLETED");
        state.setLastModifiedAt(Instant.now());
    }

    private WorkflowNode findReadyNode() {
        return state.getNodes().values().stream()
            .filter(n -> n.getStatus() == WorkflowNode.NodeStatus.PENDING || 
                         n.getStatus() == WorkflowNode.NodeStatus.READY)
            .filter(this::areDependenciesSatisfied)
            .findFirst()
            .orElse(null);
    }

    private boolean areDependenciesSatisfied(WorkflowNode node) {
        return node.getDependsOn().stream()
            .map(depId -> state.getNodes().get(depId))
            .allMatch(dep -> dep != null && dep.getStatus() == WorkflowNode.NodeStatus.SUCCEEDED);
    }

    private void executeNode(WorkflowNode node) throws WorkflowException {
        // Check approval gate if present
        if (node.getGate() != null) {
            if (!approvalManager.isGateApproved(node.getGate())) {
                node.setStatus(WorkflowNode.NodeStatus.AWAITING_APPROVAL);
                logger.info("Node {} awaiting approval at gate: {}", node.getId(), node.getGate());
                return;
            }
        }

        node.setStatus(WorkflowNode.NodeStatus.RUNNING);
        node.setStartedAt(Instant.now());

        try {
            Agent agent = agents.get(node.getOwner());
            if (agent == null) {
                throw new WorkflowException("No agent registered for owner: " + node.getOwner());
            }

            Map<String, Object> output = agent.execute(node, state);
            node.setOutput(output);
            node.setStatus(WorkflowNode.NodeStatus.SUCCEEDED);
            node.setCompletedAt(Instant.now());
            logger.info("Node {} succeeded", node.getId());
        } catch (Exception e) {
            logger.error("Node {} failed: {}", node.getId(), e.getMessage());
            handleNodeFailure(node, e);
        }
    }

    private void handleNodeFailure(WorkflowNode node, Exception e) throws WorkflowException {
        node.setLastError(e.getMessage());

        if (node.getRetryCount() < MAX_RETRIES) {
            node.incrementRetryCount();
            node.setStatus(WorkflowNode.NodeStatus.READY); // Retry
            logger.info("Node {} retry {} of {}", node.getId(), node.getRetryCount(), MAX_RETRIES);
        } else {
            node.setStatus(WorkflowNode.NodeStatus.FAILED);
            logger.error("Node {} exhausted retries after {} attempts", node.getId(), MAX_RETRIES);
            
            // Invalidate all dependent nodes
            invalidateDependents(node);
        }
    }

    private void invalidateDependents(WorkflowNode failedNode) {
        state.getNodes().values().stream()
            .filter(n -> n.getDependsOn().contains(failedNode.getId()))
            .forEach(n -> {
                n.setStatus(WorkflowNode.NodeStatus.BLOCKED);
                logger.warn("Invalidating node {} due to upstream failure: {}", n.getId(), failedNode.getId());
            });
    }

    private boolean hasBlockedNodes() {
        return state.getNodes().values().stream()
            .anyMatch(n -> n.getStatus() == WorkflowNode.NodeStatus.AWAITING_APPROVAL ||
                          n.getStatus() == WorkflowNode.NodeStatus.BLOCKED);
    }

    private boolean allNodesTerminal() {
        return state.getNodes().values().stream()
            .allMatch(n -> n.getStatus() == WorkflowNode.NodeStatus.SUCCEEDED ||
                          n.getStatus() == WorkflowNode.NodeStatus.FAILED ||
                          n.getStatus() == WorkflowNode.NodeStatus.ROLLBACK);
    }

    public static class WorkflowException extends Exception {
        public WorkflowException(String message) { super(message); }
        public WorkflowException(String message, Throwable cause) { super(message, cause); }
    }
}
