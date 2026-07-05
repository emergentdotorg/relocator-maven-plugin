package org.emergent.maven.changeling;

import lombok.Data;

/**
 * Configuration bean for a single {@code <replacement>} element: {@code from} is
 * {@code groupId:artifactId} (optionally {@code :version} or {@code :versionRange}), {@code to} is
 * {@code groupId:artifactId:version}.
 */
@Data
public class Replacement {

    private String from;
    private String to;

    public ReplacementRule toRule() {
        return ReplacementRule.of(from, to);
    }
}
