package org.emergent.maven.changeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

class ReplacerTest {

    @Test
    void directReplacementRewritesCoordinatesAndKeepsAttributes() {
        Model model = new Model();
        Dependency dependency = dependency("log4j", "log4j", "1.2.17");
        dependency.setScope("runtime");
        dependency.setOptional(true);
        model.addDependency(dependency);
        List<ReplacementRule> rules = List.of(ReplacementRule.of("log4j:log4j", "ch.qos.reload4j:reload4j:1.2.26"));

        List<String> applied = Replacer.replaceDirect(model, rules);

        assertThat(applied).containsExactly("log4j:log4j:1.2.17 -> ch.qos.reload4j:reload4j:1.2.26");
        assertThat(dependency.getGroupId()).isEqualTo("ch.qos.reload4j");
        assertThat(dependency.getArtifactId()).isEqualTo("reload4j");
        assertThat(dependency.getVersion()).isEqualTo("1.2.26");
        assertThat(dependency.getScope()).isEqualTo("runtime");
        assertThat(dependency.isOptional()).isTrue();
    }

    @Test
    void directReplacementRespectsVersionRange() {
        Model model = new Model();
        model.addDependency(dependency("log4j", "log4j", "1.2.17"));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("log4j:log4j:[1.0,1.2)", "ch.qos.reload4j:reload4j:1.2.26"));

        assertThat(Replacer.replaceDirect(model, rules)).isEmpty();
        assertThat(model.getDependencies().get(0).getGroupId()).isEqualTo("log4j");
    }

    @Test
    void transitiveReplacementExcludesCarrierAndInjectsTarget() {
        Model model = new Model();
        model.addDependency(dependency("junit", "junit", "4.13.2"));
        DependencyNode root = node(
            "test:project:1.0", null,
            node(
                "junit:junit:4.13.2", "compile",
                node("org.hamcrest:hamcrest-core:1.3", "compile")));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("org.hamcrest:hamcrest-core", "org.hamcrest:hamcrest:2.2"));

        List<String> applied = Replacer.replaceTransitive(model, root, rules);

        assertThat(applied).containsExactly(
            "org.hamcrest:hamcrest-core:1.3 -> org.hamcrest:hamcrest:2.2 (transitive via junit:junit)");
        Dependency junit = model.getDependencies().get(0);
        assertThat(junit.getExclusions())
            .extracting(Exclusion::getGroupId, Exclusion::getArtifactId)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("org.hamcrest", "hamcrest-core"));
        assertThat(model.getDependencies()).hasSize(2);
        Dependency injected = model.getDependencies().get(1);
        assertThat(injected.getGroupId()).isEqualTo("org.hamcrest");
        assertThat(injected.getArtifactId()).isEqualTo("hamcrest");
        assertThat(injected.getVersion()).isEqualTo("2.2");
        assertThat(injected.getScope()).isNull();
    }

    @Test
    void transitiveReplacementCarriesScope() {
        Model model = new Model();
        Dependency junit = dependency("junit", "junit", "4.13.2");
        junit.setScope("test");
        model.addDependency(junit);
        DependencyNode root = node(
            "test:project:1.0", null,
            node(
                "junit:junit:4.13.2", "test",
                node("org.hamcrest:hamcrest-core:1.3", "test")));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("org.hamcrest:hamcrest-core", "org.hamcrest:hamcrest:2.2"));

        Replacer.replaceTransitive(model, root, rules);

        assertThat(model.getDependencies().get(1).getScope()).isEqualTo("test");
    }

    @Test
    void transitiveReplacementSkipsInjectionWhenTargetAlreadyDeclared() {
        Model model = new Model();
        model.addDependency(dependency("junit", "junit", "4.13.2"));
        model.addDependency(dependency("org.hamcrest", "hamcrest", "2.2"));
        DependencyNode root = node(
            "test:project:1.0", null,
            node(
                "junit:junit:4.13.2", "compile",
                node("org.hamcrest:hamcrest-core:1.3", "compile")));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("org.hamcrest:hamcrest-core", "org.hamcrest:hamcrest:2.2"));

        List<String> applied = Replacer.replaceTransitive(model, root, rules);

        assertThat(applied.get(0)).contains("target already declared");
        assertThat(model.getDependencies()).hasSize(2);
        assertThat(model.getDependencies().get(0).getExclusions()).hasSize(1);
    }

    @Test
    void transitiveReplacementIgnoresDirectDependencies() {
        Model model = new Model();
        model.addDependency(dependency("log4j", "log4j", "1.2.17"));
        DependencyNode root = node(
            "test:project:1.0", null,
            node("log4j:log4j:1.2.17", "compile"));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("log4j:log4j:[1.0,1.2)", "ch.qos.reload4j:reload4j:1.2.26"));

        assertThat(Replacer.replaceTransitive(model, root, rules)).isEmpty();
        assertThat(model.getDependencies()).hasSize(1);
        assertThat(model.getDependencies().get(0).getExclusions()).isEmpty();
    }

    @Test
    void transitiveReplacementRespectsVersionRangeOnResolvedVersion() {
        Model model = new Model();
        model.addDependency(dependency("junit", "junit", "4.13.2"));
        DependencyNode root = node(
            "test:project:1.0", null,
            node(
                "junit:junit:4.13.2", "compile",
                node("org.hamcrest:hamcrest-core:1.3", "compile")));
        List<ReplacementRule> rules =
            List.of(ReplacementRule.of("org.hamcrest:hamcrest-core:[2.0,)", "org.hamcrest:hamcrest:2.2"));

        assertThat(Replacer.replaceTransitive(model, root, rules)).isEmpty();
        assertThat(model.getDependencies()).hasSize(1);
    }

    private static Dependency dependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

    private static DependencyNode node(String coordinates, String scope, DependencyNode... children) {
        DefaultDependencyNode node = scope == null
            ? new DefaultDependencyNode(new DefaultArtifact(coordinates))
            : new DefaultDependencyNode(
                new org.eclipse.aether.graph.Dependency(new DefaultArtifact(coordinates), scope));
        node.setChildren(new ArrayList<>(List.of(children)));
        return node;
    }
}
