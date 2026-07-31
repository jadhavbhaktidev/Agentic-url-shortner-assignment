package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);
    private static final int MAX_RETRIES = 3;
    private final WorkflowState state;
    private final Map<String, Agent> agents;
    private final ApprovalManager approvalManager;
    private final MeterRegistry meterRegistry;
    private final WorkflowStateStore stateStore;
    private final ExecutionPolicy executionPolicy;

    public interface Agent {
        Map<String, Object> execute(WorkflowNode node, WorkflowState state);
    }

    public WorkflowExecutor(WorkflowState state, ApprovalManager approvalManager) {
        this(state, approvalManager, null, null, ExecutionPolicy.defaultPolicy());
    }

    public WorkflowExecutor(WorkflowState state, ApprovalManager approvalManager, MeterRegistry meterRegistry) {
        this(state, approvalManager, meterRegistry, null, ExecutionPolicy.defaultPolicy());
    }

    public WorkflowExecutor(WorkflowState state, ApprovalManager approvalManager, MeterRegistry meterRegistry, WorkflowStateStore stateStore) {
        this(state, approvalManager, meterRegistry, stateStore, ExecutionPolicy.defaultPolicy());
    }

    public WorkflowExecutor(
        WorkflowState state,
        ApprovalManager approvalManager,
        MeterRegistry meterRegistry,
        WorkflowStateStore stateStore,
        ExecutionPolicy executionPolicy
    ) {
        this.state = state;
        this.agents = new HashMap<>();
        this.approvalManager = approvalManager;
        this.meterRegistry = meterRegistry;
        this.stateStore = stateStore;
        this.executionPolicy = executionPolicy != null ? executionPolicy : ExecutionPolicy.defaultPolicy();
    }

    public void registerAgent(String owner, Agent agent) {
        agents.put(owner, agent);
    }

    public void executeWorkflow() throws WorkflowException {
        recordWorkflowTransition(state.getWorkflowStatus(), "RUNNING", "workflow started");
        state.setWorkflowStatus("RUNNING");
        state.setLastModifiedAt(Instant.now());
        persistCheckpoint();

        boolean workCompleted = false;
        while (!workCompleted) {
            List<WorkflowNode> nextNodes = findReadyNodes();
            if (nextNodes.isEmpty()) {
                // Check if there are any blocked nodes or if we're done
                if (hasBlockedNodes()) {
                    logger.info("Workflow paused: nodes awaiting approval or dependencies");
                    recordWorkflowTransition(state.getWorkflowStatus(), "PAUSED", "waiting on approval or dependencies");
                    state.setWorkflowStatus("PAUSED");
                    persistCheckpoint();
                    workCompleted = true;
                } else if (allNodesTerminal()) {
                    logger.info("Workflow completed: all nodes terminal");
                    recordWorkflowTransition(state.getWorkflowStatus(), "COMPLETED", "all nodes terminal");
                    state.setWorkflowStatus("COMPLETED");
                    persistCheckpoint();
                    workCompleted = true;
                } else {
                    logger.warn("Workflow stuck: no ready nodes and unresolved blocks");
                    recordWorkflowTransition(state.getWorkflowStatus(), "STUCK", "no ready nodes and unresolved blocks");
                    state.setWorkflowStatus("STUCK");
                    persistCheckpoint();
                    workCompleted = true;
                }
            } else {
                executeReadyNodes(nextNodes);
            }
        }

        if (!"PAUSED".equals(state.getWorkflowStatus()) && !"STUCK".equals(state.getWorkflowStatus())) {
            state.setLastModifiedAt(Instant.now());
            persistCheckpoint();
        }
    }

    private WorkflowNode findReadyNode() {
        return findReadyNodes().stream().findFirst().orElse(null);
    }

    private List<WorkflowNode> findReadyNodes() {
        return state.getNodes().values().stream()
            .filter(n -> n.getStatus() == WorkflowNode.NodeStatus.PENDING ||
                         n.getStatus() == WorkflowNode.NodeStatus.READY)
            .filter(this::areDependenciesSatisfied)
            .sorted(Comparator.comparing(WorkflowNode::getId))
            .collect(Collectors.toList());
    }

    private boolean areDependenciesSatisfied(WorkflowNode node) {
        return node.getDependsOn().stream()
            .map(depId -> state.getNodes().get(depId))
            .allMatch(dep -> dep != null && dep.getStatus() == WorkflowNode.NodeStatus.SUCCEEDED);
    }

    private void executeNode(WorkflowNode node) throws WorkflowException {
        ExecutionPolicy.Decision policyDecision = executionPolicy.evaluate(node, approvalManager);
        if (!policyDecision.allowed()) {
            recordTransition(node, node.getStatus(), policyDecision.blockedStatus(), policyDecision.reason());
            node.setStatus(policyDecision.blockedStatus());
            node.setLastError(policyDecision.reason());
            incrementCounter("workflow.policy.blocks", "reason", policyDecision.reasonCode(), "node", node.getId());
            logger.warn("Policy blocked node {}: {}", node.getId(), policyDecision.reason());
            persistCheckpoint();
            return;
        }

        // Check approval gate if present
        if (node.getGate() != null) {
            if (!approvalManager.isGateApproved(node.getGate())) {
                recordTransition(node, node.getStatus(), WorkflowNode.NodeStatus.AWAITING_APPROVAL, "approval gate not approved");
                node.setStatus(WorkflowNode.NodeStatus.AWAITING_APPROVAL);
                logger.info("Node {} awaiting approval at gate: {}", node.getId(), node.getGate());
                persistCheckpoint();
                return;
            }
        }

        recordTransition(node, node.getStatus(), WorkflowNode.NodeStatus.RUNNING, "execution started");
        node.setStatus(WorkflowNode.NodeStatus.RUNNING);
        node.setStartedAt(Instant.now());
        Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;

        try {
            Agent agent = agents.get(node.getOwner());
            if (agent == null) {
                throw new WorkflowException("No agent registered for owner: " + node.getOwner());
            }

            Map<String, Object> output = agent.execute(node, state);
            node.setOutput(output);
            recordTransition(node, WorkflowNode.NodeStatus.RUNNING, WorkflowNode.NodeStatus.SUCCEEDED, "execution succeeded");
            node.setStatus(WorkflowNode.NodeStatus.SUCCEEDED);
            node.setCompletedAt(Instant.now());
            incrementCounter("workflow.node.executions", "status", "succeeded");
            if (sample != null) {
                sample.stop(Timer.builder("workflow.node.duration")
                    .tag("node", node.getId())
                    .tag("status", "succeeded")
                    .register(meterRegistry));
            }
            logger.info("Node {} succeeded", node.getId());
        } catch (Exception e) {
            logger.error("Node {} failed: {}", node.getId(), e.getMessage());
            handleNodeFailure(node, e);
        } finally {
            state.setLastModifiedAt(Instant.now());
            persistCheckpoint();
        }
    }

    private void executeReadyNodes(List<WorkflowNode> readyNodes) throws WorkflowException {
        if (readyNodes.size() == 1) {
            executeNode(readyNodes.get(0));
            return;
        }

        int workers = Math.max(1, Math.min(readyNodes.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (WorkflowNode readyNode : readyNodes) {
                futures.add(executorService.submit(() -> {
                    try {
                        executeNode(readyNode);
                    } catch (WorkflowException e) {
                        throw new IllegalStateException(e);
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new WorkflowException("Parallel node execution failed", e);
                }
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private void handleNodeFailure(WorkflowNode node, Exception e) throws WorkflowException {
        node.setLastError(e.getMessage());

        if (node.getRetryCount() < MAX_RETRIES) {
            node.incrementRetryCount();
            recordTransition(node, WorkflowNode.NodeStatus.RUNNING, WorkflowNode.NodeStatus.READY, "retry " + node.getRetryCount());
            node.setStatus(WorkflowNode.NodeStatus.READY); // Retry
            logger.info("Node {} retry {} of {}", node.getId(), node.getRetryCount(), MAX_RETRIES);
            incrementCounter("workflow.node.executions", "status", "retry");
        } else {
            recordTransition(node, WorkflowNode.NodeStatus.RUNNING, WorkflowNode.NodeStatus.FAILED, "retries exhausted");
            node.setStatus(WorkflowNode.NodeStatus.FAILED);
            logger.error("Node {} exhausted retries after {} attempts", node.getId(), MAX_RETRIES);
            incrementCounter("workflow.node.executions", "status", "failed");
            
            // Invalidate all dependent nodes
            invalidateDependents(node);
        }
    }

    private void invalidateDependents(WorkflowNode failedNode) {
        state.getNodes().values().stream()
            .filter(n -> n.getDependsOn().contains(failedNode.getId()))
            .forEach(n -> {
                recordTransition(n, n.getStatus(), WorkflowNode.NodeStatus.BLOCKED, "upstream failure: " + failedNode.getId());
                n.setStatus(WorkflowNode.NodeStatus.BLOCKED);
                logger.warn("Invalidating node {} due to upstream failure: {}", n.getId(), failedNode.getId());
            });
    }

    private void recordTransition(WorkflowNode node, WorkflowNode.NodeStatus from, WorkflowNode.NodeStatus to, String reason) {
        recordTransition(node, from != null ? from.name() : null, to.name(), reason);
    }

    private void recordTransition(WorkflowNode node, String from, String to, String reason) {
        synchronized (state) {
            state.recordTransition(from, to, node.getId(), node.getOwner(), reason);
        }
        incrementCounter("workflow.node.transitions", "from", from != null ? from : "UNKNOWN", "to", to, "node", node.getId());
    }

    private void recordWorkflowTransition(String from, String to, String reason) {
        synchronized (state) {
            state.recordTransition(from, to, "workflow", "orchestrator", reason);
        }
        incrementCounter("workflow.transitions", "from", from != null ? from : "UNKNOWN", "to", to);
    }

    private void incrementCounter(String name, String... tags) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(name)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }

    private void persistCheckpoint() throws WorkflowException {
        if (stateStore == null) {
            return;
        }
        try {
            synchronized (state) {
                stateStore.save(state);
            }
        } catch (IOException e) {
            throw new WorkflowException("Failed to persist workflow checkpoint", e);
        }
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
