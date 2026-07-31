package com.agentic.orchestration.agents;

import com.agentic.orchestration.engine.WorkflowExecutor;
import com.agentic.orchestration.model.WorkflowNode;
import com.agentic.orchestration.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationAgent implements WorkflowExecutor.Agent {
    private static final Logger logger = LoggerFactory.getLogger(ValidationAgent.class);

    @Override
    public Map<String, Object> execute(WorkflowNode node, WorkflowState state) {
        logger.info("Validation Agent executing node: {}", node.getId());

        WorkflowNode implementation = state.getNodes().get("implementation");
        if (implementation == null || implementation.getOutput() == null) {
            throw new IllegalStateException("Implementation output is required for validation");
        }

        List<String> findings = new ArrayList<>();
        boolean contractValidated = "PASSED".equalsIgnoreCase(String.valueOf(implementation.getOutput().get("apiContractValidation")));
        boolean endpointsPresent = implementation.getOutput().containsKey("endpoints");
        boolean modulesPresent = implementation.getOutput().containsKey("backendModules");

        if (!contractValidated) {
            findings.add("API contract validation did not pass");
        }
        if (!endpointsPresent) {
            findings.add("Implementation output did not include endpoints");
        }
        if (!modulesPresent) {
            findings.add("Implementation output did not include backend modules");
        }

        if (!findings.isEmpty()) {
            throw new IllegalStateException(String.join("; ", findings));
        }

        Map<String, Object> output = new HashMap<>();
        output.put("reviewStatus", "PASSED");
        output.put("contractCompliant", true);
        output.put("findings", findings);
        output.put("reviewedNode", implementation.getId());
        output.put("checkedArtifacts", List.of("apiContractValidation", "endpoints", "backendModules"));

        logger.info("Validation complete: contract compliant for node {}", implementation.getId());
        return output;
    }
}