package org.emergent.maven.changeling;

import lombok.Value;

/**
 * Maven coordinates in {@code groupId:artifactId} or {@code groupId:artifactId:version} form. The version
 * part may be a plain version, or (for rule sources) a version range such as {@code [1.0,2.0)}.
 */
@Value
public class Coordinates {

    String groupId;
    String artifactId;
    String version;

    public static Coordinates parse(String spec) {
        String[] parts = spec == null ? new String[0] : spec.trim().split(":", -1);
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException(
                    "Expected groupId:artifactId or groupId:artifactId:version but got '" + spec + "'");
        }
        for (String part : parts) {
            if (part.isBlank()) {
                throw new IllegalArgumentException("Blank segment in coordinates '" + spec + "'");
            }
        }
        return new Coordinates(parts[0], parts[1], parts.length > 2 ? parts[2] : null);
    }

    public String getGa() {
        return groupId + ":" + artifactId;
    }

    public boolean matchesGa(String otherGroupId, String otherArtifactId) {
        return groupId.equals(otherGroupId) && artifactId.equals(otherArtifactId);
    }

    @Override
    public String toString() {
        return version == null ? getGa() : getGa() + ":" + version;
    }
}
