package com.agentic.orchestration;

import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDefinitionLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadNodesFromYamlDefinition() throws Exception {
        Path dagPath = tempDir.resolve("dag.yaml");
        String yaml = """
            version: 2.0.0
            run_id: wf-yaml-001
            nodes:
              - id: requirements
                type: Requirement
                owner: requirements-analysis-agent
                status: READY
              - id: implementation
                type: Implementation
                owner: implementation-agent
                depends_on: [requirements]
                status: PENDING
            """;
        Files.writeString(dagPath, yaml, StandardCharsets.UTF_8);

        WorkflowState state = new WorkflowDefinitionLoader().load(dagPath, "fallback-run");

        assertThat(state.getRunId()).isEqualTo("wf-yaml-001");
        assertThat(state.getDagVersion()).isEqualTo("2.0.0");
        assertThat(state.getNodes()).containsKeys("requirements", "implementation");
        assertThat(state.getNodes().get("requirements").getStatus()).isEqualTo(WorkflowNode.NodeStatus.READY);
        assertThat(state.getNodes().get("implementation").getDependsOn()).containsExactly("requirements");
        assertThat(state.getApprovals()).isNotEmpty();
    }
}