package com.wegongdu.rillway.policy.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise policy document reference for agents.
 */
public record PolicyDocument(
        String id,
        String title,
        String content,
        List<String> tags
) implements Serializable {

    public PolicyDocument {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
    }

    public static PolicyDocument of(String id, String title, String content, String... tags) {
        return new PolicyDocument(id, title, content, List.of(tags));
    }
}
