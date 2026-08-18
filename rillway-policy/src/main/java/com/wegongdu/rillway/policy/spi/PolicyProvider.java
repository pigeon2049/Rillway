package com.wegongdu.rillway.policy.spi;

import com.wegongdu.rillway.policy.model.PolicyDocument;
import com.wegongdu.rillway.policy.model.PolicyQuery;
import java.util.List;
import java.util.Optional;

/**
 * Provider interface for querying enterprise policies and documents.
 */
public interface PolicyProvider {

    List<PolicyDocument> findPolicies(PolicyQuery query);

    Optional<Policy> getPolicy(String policyId);

    Optional<PolicyDocument> getPolicyDocument(String documentId);
}
