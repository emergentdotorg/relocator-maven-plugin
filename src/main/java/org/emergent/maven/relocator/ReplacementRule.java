package org.emergent.maven.relocator;

import lombok.Value;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * A single dependency replacement: artifacts matching {@code source} (exact groupId:artifactId, with an
 * optional exact version or version range) are replaced by the full {@code target} coordinates.
 */
@Value
public class ReplacementRule {

    Coordinates source;
    Coordinates target;

    public static ReplacementRule of(String sourceSpec, String targetSpec) {
        Coordinates from = Coordinates.parse(sourceSpec);
        Coordinates to = Coordinates.parse(targetSpec);
        if (to.getVersion() == null) {
            throw new IllegalArgumentException(
                    "Replacement target requires groupId:artifactId:version but got '" + targetSpec + "'");
        }
        if (isRange(from.getVersion())) {
            try {
                VersionRange.createFromVersionSpec(from.getVersion());
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException("Invalid version range in '" + sourceSpec + "'", e);
            }
        }
        return new ReplacementRule(from, to);
    }

    public boolean matches(String groupId, String artifactId, String version) {
        return source.matchesGa(groupId, artifactId) && matchesVersion(version);
    }

    private boolean matchesVersion(String version) {
        String spec = source.getVersion();
        if (spec == null) {
            return true;
        }
        if (version == null) {
            return false;
        }
        if (!isRange(spec)) {
            return spec.equals(version);
        }
        try {
            return VersionRange.createFromVersionSpec(spec).containsVersion(new DefaultArtifactVersion(version));
        } catch (InvalidVersionSpecificationException e) {
            // validated in of(), so this cannot happen
            throw new IllegalStateException(e);
        }
    }

    private static boolean isRange(String spec) {
        return spec != null && (spec.contains("[") || spec.contains("(") || spec.contains(","));
    }

    @Override
    public String toString() {
        return source + " -> " + target;
    }
}
