package org.emergent.maven.relocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReplacementRuleTest {

    @Test
    void matchesAnyVersionWhenNoVersionSpec() {
        ReplacementRule rule = ReplacementRule.of("log4j:log4j", "ch.qos.reload4j:reload4j:1.2.26");
        assertThat(rule.matches("log4j", "log4j", "1.2.17")).isTrue();
        assertThat(rule.matches("log4j", "log4j", null)).isTrue();
        assertThat(rule.matches("log4j", "other", "1.2.17")).isFalse();
    }

    @Test
    void matchesExactVersion() {
        ReplacementRule rule = ReplacementRule.of("a:b:1.5", "x:y:2.0");
        assertThat(rule.matches("a", "b", "1.5")).isTrue();
        assertThat(rule.matches("a", "b", "1.6")).isFalse();
        assertThat(rule.matches("a", "b", null)).isFalse();
    }

    @Test
    void matchesVersionRange() {
        ReplacementRule rule = ReplacementRule.of("a:b:[1.0,2.0)", "x:y:9.9");
        assertThat(rule.matches("a", "b", "1.0")).isTrue();
        assertThat(rule.matches("a", "b", "1.9.7")).isTrue();
        assertThat(rule.matches("a", "b", "2.0")).isFalse();
        assertThat(rule.matches("a", "b", "0.9")).isFalse();
    }

    @Test
    void requiresVersionOnTarget() {
        assertThatThrownBy(() -> ReplacementRule.of("a:b", "x:y"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("groupId:artifactId:version");
    }

    @Test
    void rejectsInvalidRange() {
        assertThatThrownBy(() -> ReplacementRule.of("a:b:[1.0", "x:y:1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version range");
    }

    @Test
    void rendersToString() {
        ReplacementRule rule = ReplacementRule.of("log4j:log4j", "ch.qos.reload4j:reload4j:1.2.26");
        assertThat(rule).hasToString("log4j:log4j -> ch.qos.reload4j:reload4j:1.2.26");
    }
}
