package com.wegongdu.rillway.policy.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Query for retrieving relevant enterprise policies.
 */
public record PolicyQuery(
        String queryText,
        List<String> tags,
        int topK
) implements Serializable {

    public PolicyQuery {
        tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        if (topK <= 0) {
            topK = 5;
        }
    }

    public static PolicyQuery of(String queryText) {
        return new PolicyQuery(queryText, Collections.emptyList(), 5);
    }

    public static PolicyQuery of(String queryText, List<String> tags) {
        return new PolicyQuery(queryText, tags, 5);
    }
}
