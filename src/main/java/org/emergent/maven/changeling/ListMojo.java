package org.emergent.maven.changeling;

import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Prints the configured replacement rules and, when the plugin is active as a build extension, the
 * replacements applied to the current project.
 */
@Mojo(name = "list", threadSafe = true)
public class ListMojo extends AbstractMojo {

    /**
     * The replacement rules; each {@code <replacement>} has a {@code <from>} ({@code groupId:artifactId},
     * optionally {@code :version} or {@code :versionRange}) and a {@code <to>}
     * ({@code groupId:artifactId:version}).
     */
    @Parameter
    private List<Replacement> replacements;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        if (replacements == null || replacements.isEmpty()) {
            getLog().info("No replacement rules configured.");
        } else {
            getLog().info("Configured replacement rules:");
            for (Replacement replacement : replacements) {
                try {
                    getLog().info("  " + replacement.toRule());
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid replacement rule: " + e.getMessage(), e);
                }
            }
        }
        Object applied = project.getContextValue(ChangelingLifecycleParticipant.APPLIED_CONTEXT_KEY);
        if (applied instanceof List<?> list) {
            if (list.isEmpty()) {
                getLog().info("No replacements were applied to this project.");
            } else {
                getLog().info("Applied replacements:");
                list.forEach(entry -> getLog().info("  " + entry));
            }
        } else {
            getLog().info("No replacements were applied; is the plugin declared with <extensions>true</extensions>?");
        }
    }
}
