package org.emergent.maven.relocator;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Rewrites project dependencies according to the replacement rules configured on this plugin. Runs
 * automatically when the plugin is declared with {@code <extensions>true</extensions>}: after all project
 * models are read but before any resolution, so the whole build sees the replaced dependencies.
 */
@Named("relocator-lifecycle-participant")
@Singleton
@Slf4j
public class RelocatorLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    public static final String APPLIED_CONTEXT_KEY = "relocator.applied";

    private final ProjectDependenciesResolver resolver;

    @Inject
    public RelocatorLifecycleParticipant(ProjectDependenciesResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        if (ConfigParser.isSkipped(session)) {
            log.info("relocator: dependency replacement is skipped ({})", ConfigParser.SKIP_PROPERTY);
            return;
        }
        for (MavenProject project : session.getProjects()) {
            try {
                replaceDependencies(session, project);
            } catch (IllegalArgumentException e) {
                throw new MavenExecutionException(
                        "relocator: invalid replacement configuration in " + project.getId() + ": " + e.getMessage(),
                        e);
            }
        }
    }

    private void replaceDependencies(MavenSession session, MavenProject project) {
        List<ReplacementRule> rules = ConfigParser.rulesFor(project);
        if (rules.isEmpty()) {
            return;
        }
        List<String> applied = new ArrayList<>(Replacer.replaceDirect(project.getModel(), rules));
        DependencyNode graph = collectDependencyGraph(session, project);
        if (graph != null) {
            applied.addAll(Replacer.replaceTransitive(project.getModel(), graph, rules));
        }
        applied.forEach(replacement -> log.info("relocator: {} in {}", replacement, project.getArtifactId()));
        project.setContextValue(APPLIED_CONTEXT_KEY, List.copyOf(applied));
    }

    private DependencyNode collectDependencyGraph(MavenSession session, MavenProject project) {
        DefaultDependencyResolutionRequest request =
                new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
        // reject everything from artifact resolution: only the collected graph (pom metadata) is needed
        request.setResolutionFilter((node, parents) -> false);
        try {
            return resolver.resolve(request).getDependencyGraph();
        } catch (DependencyResolutionException e) {
            DependencyResolutionResult result = e.getResult();
            DependencyNode graph = result == null ? null : result.getDependencyGraph();
            if (graph == null) {
                log.warn(
                        "relocator: could not collect dependency graph of {}; transitive replacement skipped: {}",
                        project.getId(), e.getMessage());
            } else {
                log.debug(
                        "relocator: dependency graph of {} collected partially: {}",
                        project.getId(),
                        e.getMessage());
            }
            return graph;
        }
    }
}
