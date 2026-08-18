package com.wegongdu.rillway.core.validation;

import com.wegongdu.rillway.core.definition.Edge;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.NodeType;
import com.wegongdu.rillway.core.node.AgentNode;
import com.wegongdu.rillway.core.node.EndNode;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.node.RuleNode;
import com.wegongdu.rillway.core.node.StartNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Standard implementation of ProcessValidator.
 */
public class StandardProcessValidator implements ProcessValidator {

    @Override
    public ValidationResult validate(ProcessDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();

        if (definition == null) {
            return ValidationResult.invalid(ValidationError.of("DEFINITION_NULL", "ProcessDefinition must not be null"));
        }

        Map<String, Node> nodes = definition.nodes();
        if (nodes.isEmpty()) {
            return ValidationResult.invalid(ValidationError.of("NO_NODES", "ProcessDefinition contains no nodes"));
        }

        // 1. Check StartNode
        List<StartNode> startNodes = nodes.values().stream()
                .filter(n -> n.type() == NodeType.START)
                .map(n -> (StartNode) n)
                .toList();

        if (startNodes.isEmpty()) {
            errors.add(ValidationError.of("MISSING_START_NODE", "ProcessDefinition must contain at least one StartNode"));
        } else if (startNodes.size() > 1) {
            errors.add(ValidationError.of("MULTIPLE_START_NODES", "ProcessDefinition must contain only one StartNode"));
        }

        // 2. Check EndNode
        boolean hasEndNode = nodes.values().stream().anyMatch(n -> n.type() == NodeType.END);
        if (!hasEndNode) {
            errors.add(ValidationError.of("MISSING_END_NODE", "ProcessDefinition must contain at least one EndNode"));
        }

        // 3. Check Edges validity
        for (Edge edge : definition.edges()) {
            if (!nodes.containsKey(edge.sourceNodeId())) {
                errors.add(ValidationError.of("INVALID_EDGE_SOURCE", "Edge source node does not exist: " + edge.sourceNodeId()));
            }
            if (!nodes.containsKey(edge.targetNodeId())) {
                errors.add(ValidationError.of("INVALID_EDGE_TARGET", "Edge target node does not exist: " + edge.targetNodeId()));
            }
        }

        // 4. Validate AgentNodes & RuleNodes internal references
        for (Node node : nodes.values()) {
            if (node instanceof AgentNode agentNode) {
                if (agentNode.agentId() == null || agentNode.agentId().isBlank()) {
                    errors.add(ValidationError.of("AGENT_ID_EMPTY", "AgentNode must specify a valid agentId", agentNode.id()));
                }
                if (agentNode.authority() == null) {
                    errors.add(ValidationError.of("AGENT_AUTHORITY_NULL", "AgentNode must declare an AgentAuthority", agentNode.id()));
                }
                if (agentNode.allowedDecisions() == null || agentNode.allowedDecisions().isEmpty()) {
                    errors.add(ValidationError.of("AGENT_ALLOWED_DECISIONS_EMPTY", "AgentNode allowedDecisions must not be empty", agentNode.id()));
                }
                if (agentNode.fallbackNodeId() != null && !nodes.containsKey(agentNode.fallbackNodeId())) {
                    errors.add(ValidationError.of("AGENT_FALLBACK_NODE_NOT_FOUND", "Agent fallbackNodeId does not exist: " + agentNode.fallbackNodeId(), agentNode.id()));
                }
                for (Map.Entry<?, String> entry : agentNode.decisionRoutes().entrySet()) {
                    if (!nodes.containsKey(entry.getValue())) {
                        errors.add(ValidationError.of("AGENT_ROUTE_TARGET_NOT_FOUND", "Agent decision route target does not exist: " + entry.getValue(), agentNode.id()));
                    }
                }
                if (agentNode.defaultTargetNodeId() != null && !nodes.containsKey(agentNode.defaultTargetNodeId())) {
                    errors.add(ValidationError.of("AGENT_DEFAULT_TARGET_NOT_FOUND", "Agent default target node does not exist: " + agentNode.defaultTargetNodeId(), agentNode.id()));
                }
            } else if (node instanceof RuleNode ruleNode) {
                for (RuleNode.RuleBranch branch : ruleNode.branches()) {
                    if (!nodes.containsKey(branch.targetNodeId())) {
                        errors.add(ValidationError.of("RULE_BRANCH_TARGET_NOT_FOUND", "Rule branch target node does not exist: " + branch.targetNodeId(), ruleNode.id()));
                    }
                }
                if (ruleNode.defaultTargetNodeId() != null && !nodes.containsKey(ruleNode.defaultTargetNodeId())) {
                    errors.add(ValidationError.of("RULE_DEFAULT_TARGET_NOT_FOUND", "Rule default target node does not exist: " + ruleNode.defaultTargetNodeId(), ruleNode.id()));
                }
            }
        }

        // 5. Reachability Check
        if (!startNodes.isEmpty()) {
            StartNode startNode = startNodes.get(0);
            Set<String> reachableNodeIds = computeReachableNodes(startNode.id(), definition);
            for (String nodeId : nodes.keySet()) {
                if (!reachableNodeIds.contains(nodeId)) {
                    errors.add(ValidationError.of("UNREACHABLE_NODE", "Node is unreachable from start node: " + nodeId, nodeId));
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private Set<String> computeReachableNodes(String startNodeId, ProcessDefinition definition) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        visited.add(startNodeId);
        queue.add(startNodeId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Node node = definition.nodes().get(currentId);
            if (node == null) continue;

            List<String> nextTargets = new ArrayList<>();

            // 1. From standard edges
            for (Edge edge : definition.outgoingEdges(currentId)) {
                nextTargets.add(edge.targetNodeId());
            }

            // 2. From RuleNode internal branches
            if (node instanceof RuleNode rn) {
                for (RuleNode.RuleBranch branch : rn.branches()) {
                    nextTargets.add(branch.targetNodeId());
                }
                if (rn.defaultTargetNodeId() != null) {
                    nextTargets.add(rn.defaultTargetNodeId());
                }
            }

            // 3. From AgentNode internal routes & fallback
            if (node instanceof AgentNode an) {
                nextTargets.addAll(an.decisionRoutes().values());
                if (an.defaultTargetNodeId() != null) {
                    nextTargets.add(an.defaultTargetNodeId());
                }
                if (an.fallbackNodeId() != null) {
                    nextTargets.add(an.fallbackNodeId());
                }
            }

            for (String target : nextTargets) {
                if (definition.nodes().containsKey(target) && visited.add(target)) {
                    queue.add(target);
                }
            }
        }

        return visited;
    }
}
