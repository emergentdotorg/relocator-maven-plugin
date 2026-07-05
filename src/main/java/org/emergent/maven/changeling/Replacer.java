package org.emergent.maven.changeling;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Applies replacement rules to a project model. The direct pass rewrites matching declared dependencies
 * in place. The transitive pass inspects a collected dependency graph: for every rule matched somewhere
 * below a direct dependency, it excludes the matched artifact from the direct dependencies that carry it
 * and injects the replacement as a new direct dependency.
 */
public final class Replacer {

    private static final Map<String, Integer> SCOPE_PRIORITY =
            Map.of("compile", 4, "runtime", 3, "provided", 2, "system", 2, "test", 1);

    private Replacer() {
    }

    public static List<String> replaceDirect(Model model, List<ReplacementRule> rules) {
        List<String> applied = new ArrayList<>();
        for (Dependency dependency : model.getDependencies()) {
            for (ReplacementRule rule : rules) {
                if (rule.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
                    String original = dependency.getGroupId() + ":" + dependency.getArtifactId()
                            + (dependency.getVersion() == null ? "" : ":" + dependency.getVersion());
                    Coordinates to = rule.getTo();
                    dependency.setGroupId(to.getGroupId());
                    dependency.setArtifactId(to.getArtifactId());
                    dependency.setVersion(to.getVersion());
                    applied.add(original + " -> " + to);
                    break;
                }
            }
        }
        return applied;
    }

    public static List<String> replaceTransitive(Model model, DependencyNode root, List<ReplacementRule> rules) {
        List<String> applied = new ArrayList<>();
        for (ReplacementRule rule : rules) {
            TransitiveMatches matches = findMatches(root, rule);
            if (matches.carrierGas.isEmpty()) {
                continue;
            }
            excludeFromCarriers(model, rule, matches.carrierGas);
            boolean injected = injectReplacement(model, rule, matches.widestScope());
            applied.add(rule.getFrom().getGa() + ":" + String.join(",", matches.versions) + " -> " + rule.getTo()
                    + " (transitive via " + String.join(", ", matches.carrierGas)
                    + (injected ? "" : "; target already declared") + ")");
        }
        return applied;
    }

    private static TransitiveMatches findMatches(DependencyNode root, ReplacementRule rule) {
        TransitiveMatches matches = new TransitiveMatches();
        for (DependencyNode direct : root.getChildren()) {
            collectMatchesBelow(direct, direct, rule, matches);
        }
        return matches;
    }

    private static void collectMatchesBelow(
            DependencyNode carrier, DependencyNode node, ReplacementRule rule, TransitiveMatches matches) {
        for (DependencyNode child : node.getChildren()) {
            Artifact artifact = child.getArtifact();
            if (artifact != null && rule.matches(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion())) {
                Artifact carrierArtifact = carrier.getArtifact();
                if (carrierArtifact != null) {
                    matches.carrierGas.add(carrierArtifact.getGroupId() + ":" + carrierArtifact.getArtifactId());
                }
                matches.versions.add(artifact.getVersion());
                matches.scopes.add(child.getDependency() == null ? "" : child.getDependency().getScope());
            }
            collectMatchesBelow(carrier, child, rule, matches);
        }
    }

    private static void excludeFromCarriers(Model model, ReplacementRule rule, Set<String> carrierGas) {
        Coordinates from = rule.getFrom();
        for (Dependency dependency : model.getDependencies()) {
            String ga = dependency.getGroupId() + ":" + dependency.getArtifactId();
            if (!carrierGas.contains(ga)) {
                continue;
            }
            boolean alreadyExcluded = dependency.getExclusions().stream()
                    .anyMatch(exclusion -> from.matchesGa(exclusion.getGroupId(), exclusion.getArtifactId()));
            if (!alreadyExcluded) {
                Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(from.getGroupId());
                exclusion.setArtifactId(from.getArtifactId());
                dependency.addExclusion(exclusion);
            }
        }
    }

    private static boolean injectReplacement(Model model, ReplacementRule rule, String scope) {
        Coordinates to = rule.getTo();
        boolean alreadyDeclared = model.getDependencies().stream()
                .anyMatch(dependency -> to.matchesGa(dependency.getGroupId(), dependency.getArtifactId()));
        if (alreadyDeclared) {
            return false;
        }
        Dependency dependency = new Dependency();
        dependency.setGroupId(to.getGroupId());
        dependency.setArtifactId(to.getArtifactId());
        dependency.setVersion(to.getVersion());
        if (!"compile".equals(scope)) {
            dependency.setScope(scope);
        }
        model.addDependency(dependency);
        return true;
    }

    private static final class TransitiveMatches {

        final Set<String> carrierGas = new LinkedHashSet<>();
        final Set<String> versions = new LinkedHashSet<>();
        final Set<String> scopes = new LinkedHashSet<>();

        String widestScope() {
            return scopes.stream()
                    .map(scope -> scope == null || scope.isEmpty() ? "compile" : scope)
                    .max(java.util.Comparator.comparingInt(scope -> SCOPE_PRIORITY.getOrDefault(scope, 0)))
                    .orElse("compile");
        }
    }
}
