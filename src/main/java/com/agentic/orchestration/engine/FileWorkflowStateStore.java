package com.agentic.orchestration.engine;

import com.agentic.orchestration.model.WorkflowState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileWorkflowStateStore implements WorkflowStateStore {
    private final Path checkpointDirectory;

    public FileWorkflowStateStore(Path checkpointDirectory) {
        this.checkpointDirectory = checkpointDirectory;
    }

    @Override
    public void save(WorkflowState state) throws IOException {
        Files.createDirectories(checkpointDirectory);
        Path checkpointPath = checkpointDirectory.resolve(state.getRunId() + ".json");
        Files.writeString(checkpointPath, state.toJson(), StandardCharsets.UTF_8);
    }

    @Override
    public Optional<WorkflowState> load(String runId) throws IOException {
        Path checkpointPath = checkpointDirectory.resolve(runId + ".json");
        return load(checkpointPath);
    }

    @Override
    public Optional<WorkflowState> load(Path checkpointPath) throws IOException {
        if (!Files.exists(checkpointPath)) {
            return Optional.empty();
        }
        String json = Files.readString(checkpointPath, StandardCharsets.UTF_8);
        return Optional.of(WorkflowState.fromJson(json));
    }
}