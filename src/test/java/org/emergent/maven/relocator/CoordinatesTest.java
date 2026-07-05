package org.emergent.maven.relocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoordinatesTest {

    @Test
    void parsesGroupIdArtifactId() {
        Coordinates coordinates = Coordinates.parse("log4j:log4j");
        assertThat(coordinates.getGroupId()).isEqualTo("log4j");
        assertThat(coordinates.getArtifactId()).isEqualTo("log4j");
        assertThat(coordinates.getVersion()).isNull();
        assertThat(coordinates.getGa()).isEqualTo("log4j:log4j");
    }

    @Test
    void parsesFullCoordinates() {
        Coordinates coordinates = Coordinates.parse("ch.qos.reload4j:reload4j:1.2.26");
        assertThat(coordinates.getVersion()).isEqualTo("1.2.26");
        assertThat(coordinates).hasToString("ch.qos.reload4j:reload4j:1.2.26");
    }

    @Test
    void parsesVersionRange() {
        Coordinates coordinates = Coordinates.parse("log4j:log4j:[1.0,2.0)");
        assertThat(coordinates.getVersion()).isEqualTo("[1.0,2.0)");
    }

    @Test
    void trimsWhitespace() {
        assertThat(Coordinates.parse("  a:b  ").getGa()).isEqualTo("a:b");
    }

    @Test
    void rejectsMalformedSpecs() {
        assertThatThrownBy(() -> Coordinates.parse("justone")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Coordinates.parse("a:b:c:d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Coordinates.parse("a::c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Coordinates.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Coordinates.parse(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void matchesGaExactly() {
        Coordinates coordinates = Coordinates.parse("a:b");
        assertThat(coordinates.matchesGa("a", "b")).isTrue();
        assertThat(coordinates.matchesGa("a", "x")).isFalse();
        assertThat(coordinates.matchesGa("x", "b")).isFalse();
    }
}
