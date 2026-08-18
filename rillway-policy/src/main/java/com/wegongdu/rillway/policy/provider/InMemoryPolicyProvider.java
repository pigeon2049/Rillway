package com.wegongdu.rillway.policy.provider;

import com.wegongdu.rillway.policy.model.PolicyDocument;
import com.wegongdu.rillway.policy.model.PolicyQuery;
import com.wegongdu.rillway.policy.spi.Policy;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PolicyProvider.
 */
public class InMemoryPolicyProvider implements PolicyProvider {

    private final Map<String, Policy> policies = new ConcurrentHashMap<>();
    private final Map<String, PolicyDocument> documents = new ConcurrentHashMap<>();

    public InMemoryPolicyProvider registerPolicy(Policy policy) {
        if (policy != null) {
            policies.put(policy.id(), policy);
        }
        return this;
    }

    public InMemoryPolicyProvider registerDocument(PolicyDocument document) {
        if (document != null) {
            documents.put(document.id(), document);
        }
        return this;
    }

    @Override
    public List<PolicyDocument> findPolicies(PolicyQuery query) {
        if (query == null) {
            return List.copyOf(documents.values());
        }
        return documents.values().stream()
                .filter(doc -> {
                    if (query.tags() != null && !query.tags().isEmpty()) {
                        boolean matchTag = query.tags().stream().anyMatch(doc.tags()::contains);
                        if (!matchTag) return false;
                    }
                    if (query.queryText() != null && !query.queryText().isBlank()) {
                        String lowerQuery = query.queryText().toLowerCase();
                        return doc.title().toLowerCase().contains(lowerQuery)
                                || (doc.content() != null && doc.content().toLowerCase().contains(lowerQuery));
                    }
                    return true;
                })
                .limit(query.topK())
                .toList();
    }

    @Override
    public Optional<Policy> getPolicy(String policyId) {
        return Optional.ofNullable(policies.get(policyId));
    }

    @Override
    public Optional<PolicyDocument> getPolicyDocument(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }
}
