package org.emergent.maven.changeling;

import lombok.Value;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * A single dependency replacement: artifacts matching {@code from} (exact groupId:artifactId, with an
 * optional exact version or version range) are replaced by the full {@code to} coordinates.
 */
@Value
public class ReplacementRule {

    Coordinates from;
    Coordinates to;

    public static ReplacementRule of(String fromSpec, String toSpec) {
        Coordinates from = Coordinates.parse(fromSpec);
        Coordinates to = Coordinates.parse(toSpec);
        if (to.getVersion() == null) {
            throw new IllegalArgumentException(
                    "Replacement target requires groupId:artifactId:version but got '" + toSpec + "'");
        }
        if (isRange(from.getVersion())) {
            try {
                VersionRange.createFromVersionSpec(from.getVersion());
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException("Invalid version range in '" + fromSpec + "'", e);
            }
        }
        return new ReplacementRule(from, to);
    }

    public boolean matches(String groupId, String artifactId, String version) {
        return from.matchesGa(groupId, artifactId) && matchesVersion(version);
    }

    private boolean matchesVersion(String version) {
        String spec = from.getVersion();
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
        return from + " -> " + to;
    }
}
