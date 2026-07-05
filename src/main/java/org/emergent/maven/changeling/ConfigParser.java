package org.emergent.maven.changeling;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Reads replacement rules from this plugin's {@code <configuration>} in a project's build plugins.
 */
public final class ConfigParser {

    public static final String PLUGIN_GROUP_ID = "org.emergent.maven.plugins";
    public static final String PLUGIN_ARTIFACT_ID = "changeling-maven-plugin";
    public static final String SKIP_PROPERTY = "changeling.skip";
    public static final String SKIP_ENV = "CHANGELING_SKIP";

    private ConfigParser() {
    }

    public static boolean isSkipped(MavenSession session) {
        String value = session.getUserProperties().getProperty(SKIP_PROPERTY);
        if (value == null) {
            value = System.getProperty(SKIP_PROPERTY);
        }
        if (value == null) {
            value = System.getenv(SKIP_ENV);
        }
        return Boolean.parseBoolean(value);
    }

    public static List<ReplacementRule> rulesFor(MavenProject project) {
        return project.getBuildPlugins().stream()
                .filter(ConfigParser::isChangelingPlugin)
                .findFirst()
                .map(plugin -> parseRules((Xpp3Dom) plugin.getConfiguration()))
                .orElse(List.of());
    }

    private static boolean isChangelingPlugin(Plugin plugin) {
        return PLUGIN_GROUP_ID.equals(plugin.getGroupId()) && PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId());
    }

    private static List<ReplacementRule> parseRules(Xpp3Dom configuration) {
        if (configuration == null) {
            return List.of();
        }
        Xpp3Dom replacements = configuration.getChild("replacements");
        if (replacements == null) {
            return List.of();
        }
        List<ReplacementRule> rules = new ArrayList<>();
        for (Xpp3Dom replacement : replacements.getChildren("replacement")) {
            rules.add(ReplacementRule.of(childValue(replacement, "from"), childValue(replacement, "to")));
        }
        return List.copyOf(rules);
    }

    private static String childValue(Xpp3Dom replacement, String name) {
        Xpp3Dom child = replacement.getChild(name);
        String value = child == null ? null : child.getValue();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing <" + name + "> in <replacement> configuration");
        }
        return value.trim();
    }
}
