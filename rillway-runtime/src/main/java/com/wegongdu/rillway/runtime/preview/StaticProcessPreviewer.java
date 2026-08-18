package com.wegongdu.rillway.runtime.preview;

import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.node.AgentNode;
import com.wegongdu.rillway.core.node.HumanNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.node.RuleNode;
import com.wegongdu.rillway.core.node.StartNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static path previewer analyzing reachable paths based on provided form data and definition structure.
 */
public class StaticProcessPreviewer implements ProcessPreviewer {

    @Override
    public ProcessPreview preview(ProcessDefinition definition, PreviewContext context) {
        if (definition == null) {
            return new ProcessPreview("null", "null", List.of(), List.of(), List.of(), List.of(), List.of(), List.of("Definition is null"));
        }

        List<String> potentialPath = new ArrayList<>();
        List<String> humanApprovers = new ArrayList<>();
        List<String> agentNodes = new ArrayList<>();
        List<String> rules = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        StartNode startNode = definition.getStartNode();
        if (startNode == null) {
            warnings.add("Process has no StartNode configured");
            return new ProcessPreview(definition.id(), definition.name(), potentialPath, humanApprovers, agentNodes, rules, List.of(), warnings);
        }

        // Trace path heuristically
        String currentId = startNode.id();
        Set<String> visited = new HashSet<>();

        while (currentId != null && visited.add(currentId)) {
            potentialPath.add(currentId);
            Node node = definition.nodes().get(currentId);
            if (node == null) break;

            if (node instanceof HumanNode hn) {
                if (hn.assigneeRole() != null) humanApprovers.add("Role: " + hn.assigneeRole());
                if (hn.assigneeUser() != null) humanApprovers.add("User: " + hn.assigneeUser());
                currentId = getFirstOutgoingTarget(definition, currentId);
            } else if (node instanceof AgentNode an) {
                agentNodes.add(String.format("Agent [%s] (Authority: %s)", an.agentId(), an.authority()));
                if (an.defaultTargetNodeId() != null) {
                    currentId = an.defaultTargetNodeId();
                } else if (!an.decisionRoutes().isEmpty()) {
                    currentId = an.decisionRoutes().values().iterator().next();
                } else {
                    currentId = getFirstOutgoingTarget(definition, currentId);
                }
            } else if (node instanceof RuleNode rn) {
                rules.add(rn.name());
                String nextTarget = null;
                if (context != null && context.variables() != null) {
                    for (RuleNode.RuleBranch branch : rn.branches()) {
                        try {
                            if (branch.condition().test(context.variables())) {
                                nextTarget = branch.targetNodeId();
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (nextTarget == null) {
                    nextTarget = rn.defaultTargetNodeId() != null ? rn.defaultTargetNodeId() : getFirstOutgoingTarget(definition, currentId);
                }
                currentId = nextTarget;
            } else {
                currentId = getFirstOutgoingTarget(definition, currentId);
            }
        }

        return new ProcessPreview(
                definition.id(),
                definition.name(),
                potentialPath,
                humanApprovers,
                agentNodes,
                rules,
                List.of(),
                warnings
        );
    }

    private String getFirstOutgoingTarget(ProcessDefinition definition, String sourceId) {
        List<Edge> edges = definition.outgoingEdges(sourceId);
        return edges.isEmpty() ? null : edges.get(0).targetNodeId();
    }
}
