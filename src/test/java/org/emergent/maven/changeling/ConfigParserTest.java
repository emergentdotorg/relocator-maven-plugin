package org.emergent.maven.changeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

class ConfigParserTest {

    @Test
    void parsesRulesFromPluginConfiguration() {
        MavenProject project = projectWithConfiguration(configuration(
            replacement("log4j:log4j", "ch.qos.reload4j:reload4j:1.2.26"),
            replacement("a:b:[1.0,2.0)", "x:y:3.0")));

        List<ReplacementRule> rules = ConfigParser.rulesFor(project);

        assertThat(rules).hasSize(2);
        assertThat(rules.get(0)).hasToString("log4j:log4j -> ch.qos.reload4j:reload4j:1.2.26");
        assertThat(rules.get(1)).hasToString("a:b:[1.0,2.0) -> x:y:3.0");
    }

    @Test
    void returnsEmptyWhenPluginAbsent() {
        assertThat(ConfigParser.rulesFor(new MavenProject())).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoConfiguration() {
        assertThat(ConfigParser.rulesFor(projectWithConfiguration(null))).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoReplacements() {
        assertThat(ConfigParser.rulesFor(projectWithConfiguration(new Xpp3Dom("configuration")))).isEmpty();
    }

    @Test
    void failsOnIncompleteReplacement() {
        Xpp3Dom replacement = new Xpp3Dom("replacement");
        Xpp3Dom from = new Xpp3Dom("from");
        from.setValue("a:b");
        replacement.addChild(from);
        MavenProject project = projectWithConfiguration(configuration(replacement));

        assertThatThrownBy(() -> ConfigParser.rulesFor(project))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("<to>");
    }

    private static MavenProject projectWithConfiguration(Xpp3Dom configuration) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(ConfigParser.PLUGIN_GROUP_ID);
        plugin.setArtifactId(ConfigParser.PLUGIN_ARTIFACT_ID);
        plugin.setConfiguration(configuration);
        Build build = new Build();
        build.addPlugin(plugin);
        Model model = new Model();
        model.setBuild(build);
        return new MavenProject(model);
    }

    private static Xpp3Dom configuration(Xpp3Dom... replacementElements) {
        Xpp3Dom replacements = new Xpp3Dom("replacements");
        for (Xpp3Dom replacementElement : replacementElements) {
            replacements.addChild(replacementElement);
        }
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(replacements);
        return configuration;
    }

    private static Xpp3Dom replacement(String fromValue, String toValue) {
        Xpp3Dom from = new Xpp3Dom("from");
        from.setValue(fromValue);
        Xpp3Dom to = new Xpp3Dom("to");
        to.setValue(toValue);
        Xpp3Dom replacement = new Xpp3Dom("replacement");
        replacement.addChild(from);
        replacement.addChild(to);
        return replacement;
    }
}
