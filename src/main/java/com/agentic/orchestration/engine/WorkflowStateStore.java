package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowState;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface WorkflowStateStore {
    void save(WorkflowState state) throws IOException;
    Optional<WorkflowState> load(String runId) throws IOException;
    Optional<WorkflowState> load(Path checkpointPath) throws IOException;
}