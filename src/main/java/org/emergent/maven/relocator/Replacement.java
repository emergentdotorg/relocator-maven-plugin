package org.emergent.maven.relocator;

import lombok.Data;

/**
 * Configuration bean for a single {@code <replacement>} element: {@code source} is
 * {@code groupId:artifactId} (optionally {@code :version} or {@code :versionRange}), {@code target} is
 * {@code groupId:artifactId:version}.
 */
@Data
public class Replacement {

    private String source;
    private String target;

    public ReplacementRule toRule() {
        return ReplacementRule.of(source, target);
    }
}
