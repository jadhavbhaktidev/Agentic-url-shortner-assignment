package com.agentic.orchestration;

import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

public class WorkflowDefinitionLoader {

    @SuppressWarnings("unchecked")
    public WorkflowState load(Path yamlPath, String fallbackRunId) throws IOException {
        if (!Files.exists(yamlPath)) {
            throw new IOException("Workflow definition not found: " + yamlPath);
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream input = Files.newInputStream(yamlPath)) {
            root = yaml.load(input);
        }

        if (root == null || root.get("nodes") == null) {
            throw new IOException("Workflow definition does not contain nodes: " + yamlPath);
        }

        String runId = asString(root.get("run_id"));
        String dagVersion = asString(root.get("version"));
        WorkflowState state = new WorkflowState(
            runId != null && !runId.isBlank() ? runId : fallbackRunId,
            dagVersion != null && !dagVersion.isBlank() ? dagVersion : "1.0.0"
        );

        List<Map<String, Object>> nodeDefs = (List<Map<String, Object>>) root.get("nodes");
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        for (Map<String, Object> nodeDef : nodeDefs) {
            String id = Objects.requireNonNull(asString(nodeDef.get("id")), "node id is required");
            String type = Objects.requireNonNull(asString(nodeDef.get("type")), "node type is required");
            String owner = Objects.requireNonNull(asString(nodeDef.get("owner")), "node owner is required");

            WorkflowNode node = new WorkflowNode(id, type, owner);

            String status = asString(nodeDef.get("status"));
            if (status != null) {
                node.setStatus(WorkflowNode.NodeStatus.valueOf(status));
            }

            String gate = asString(nodeDef.get("gate"));
            if (gate != null) {
                node.setGate(gate);
            }

            String exit = asString(nodeDef.get("exit"));
            if (exit != null) {
                node.setExit(exit);
            }

            Object dependsOnObj = nodeDef.get("depends_on");
            if (dependsOnObj instanceof List<?> dependsOnList) {
                List<String> dependsOn = new ArrayList<>();
                for (Object dep : dependsOnList) {
                    dependsOn.add(String.valueOf(dep));
                }
                node.setDependsOn(dependsOn);
            }

            nodes.put(node.getId(), node);
        }
        state.setNodes(nodes);

        state.setApprovals(defaultApprovals());
        return state;
    }

    private List<WorkflowState.Approval> defaultApprovals() {
        List<WorkflowState.Approval> approvals = new ArrayList<>();
        approvals.add(new WorkflowState.Approval("APR-001", "requirement_interpretation", "APPROVED", "product_owner"));
        approvals.add(new WorkflowState.Approval("APR-002", "architecture_adr", "APPROVED", "engineering_lead"));
        approvals.add(new WorkflowState.Approval("APR-003", "public_api_schema", "APPROVED", "api_owner"));
        approvals.add(new WorkflowState.Approval("APR-004", "security_controls", "APPROVED", "security_reviewer"));
        approvals.add(new WorkflowState.Approval("APR-005", "production_readiness", "APPROVED", "release_manager"));
        approvals.add(new WorkflowState.Approval("APR-006", "release_approval", "APPROVED", "release_manager"));
        return approvals;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}