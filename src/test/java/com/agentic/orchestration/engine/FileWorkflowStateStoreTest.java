package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileWorkflowStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndLoadCheckpointByRunId() throws Exception {
        WorkflowState state = new WorkflowState("wf-store-001", "1.0.0");
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        WorkflowNode node = new WorkflowNode("requirements", "Requirement", "requirements-analysis-agent");
        node.setStatus(WorkflowNode.NodeStatus.SUCCEEDED);
        nodes.put(node.getId(), node);
        state.setNodes(nodes);

        FileWorkflowStateStore store = new FileWorkflowStateStore(tempDir);
        store.save(state);

        Path checkpointPath = tempDir.resolve("wf-store-001.json");
        assertThat(Files.exists(checkpointPath)).isTrue();

        WorkflowState restored = store.load("wf-store-001").orElseThrow();
        assertThat(restored.getRunId()).isEqualTo("wf-store-001");
        assertThat(restored.getNodes().get("requirements").getStatus()).isEqualTo(WorkflowNode.NodeStatus.SUCCEEDED);
    }
}