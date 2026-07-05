# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## What this is

Relocator (`org.emergent.maven.relocator:relocator-maven-plugin`) is a single-module Maven plugin that replaces
configured dependencies with substitutes during the build — conceptually Maven dependency
relocation, but driven by plugin `<configuration>` instead of relocation POMs in a repository.
Users declare it with `<extensions>true</extensions>` and it rewrites dependencies transparently,
including artifacts that only appear transitively. See `README.md` for the rule syntax and
user-facing semantics.

## Build & test

Always use the Maven wrapper (`./mvnw`), Java 17.

```bash
./mvnw verify                          # build + unit tests (JUnit 5 + AssertJ)
./mvnw test -Dtest=ReplacerTest        # one test class
./mvnw test -Dtest=ReplacerTest#transitiveReplacementCarriesScope  # one test method
./mvnw verify -Prun-its                # integration tests (maven-invoker-plugin, needs network)
```

The parent pom (`org.emergent.parent:maven-parent:0.0.2`) imposes gates that fail the build:
checkstyle at `validate` (emergent rules: 120-column lines, 2-space indent, no tabs, newline at
EOF), `dependency:analyze-only` with `failOnWarning` (every compile/test-scope class used must be
a declared dependency — e.g., declare `junit-jupiter-api`/`-engine` separately, not the
`junit-jupiter` aggregate), and enforcer `requireUpperBoundDeps`.

## Architecture

All logic lives in one package, `org.emergent.maven.relocator`. The essential mechanism spans
several pieces that must stay consistent:

- **`ChangelingLifecycleParticipant`** is the entry point. Because the consuming project declares the
  plugin with `<extensions>true</extensions>`, Maven loads the plugin realm as a build extension
  and invokes this `AbstractMavenLifecycleParticipant` at `afterProjectsRead` — after models are
  interpolated, before any resolution — where mutating `project.getModel()` reliably changes what
  the whole build resolves. It runs two passes per project via **`Replacer`**:
  1. *Direct pass*: rewrites matching declared `Dependency` objects in place (GAV swapped,
     scope/type/classifier/optional/exclusions preserved).
  2. *Transitive pass*: collects the dependency graph through the injected
     `ProjectDependenciesResolver` using a reject-all `DependencyFilter` (the collection reads POM
     metadata only, no jar downloads — keep it that way). For rules matched at depth ≥ 2 it adds
     an `<exclusion>` to each direct dependency carrying the match and injects the replacement as
     a direct dependency with the widest matched scope. Depth-1 nodes are deliberately ignored:
     they were already handled (or intentionally not matched, e.g., by version range) in the
     direct pass.
- **Discovery depends on the sisu index.** `META-INF/sisu/javax.inject.Named` is generated at
  compile time by the annotation processor inside the *provided* `org.eclipse.sisu.inject`
  dependency — there is no sisu-maven-plugin execution. If that dependency is removed or
  annotation processing is disabled, the participant silently stops being discovered (mojos still
  work via `plugin.xml`). The `direct-replacement` IT is what catches this.
- **Configuration is parsed twice, by design.** The participant reads the raw `Xpp3Dom` from
  `project.getBuildPlugins()` via `ConfigParser` (participants get no parameter injection); the
  `ListMojo` goal gets the same schema via normal `@Parameter` mapping onto the `Replacement`
  bean. Both funnel into `ReplacementRule`/`Coordinates` (matching and validation). A schema change
  must touch both paths.
- The participant stashes applied-replacement summaries in a project context value
  (`ChangelingLifecycleParticipant.APPLIED_CONTEXT_KEY`) which `relocator:list` reads back.

Known limitation (documented in README, relevant when reasoning about the scope of changes): the
rewrite affects only the in-memory model of the current build; the installed/deployed POM is the
original file, so consumers don't inherit replacements.

## Integration tests

Real behavior is verified end-to-end under the `run-its` profile: each scenario in
`src/it/<name>/` is a self-contained project whose pom declares relocator-maven-plugin (version token
`@project.version@`, filtered by invoker) plus a `dependency:list` execution writing
`target/deps.txt`; `verify.groovy` asserts on that file and on `build.log`. Per-scenario flags go
in `invoker.properties` (see `skip-flag`). When changing replacement semantics, add or update an
`src/it/` scenario, not just a unit test. Invoker installs the freshly built plugin into
`target/local-invoker-repo`; sample artifacts are pre-seeded via `<extraArtifacts>` in the pom's
`run-its` profile.

## Conventions

- Version is a plain literal (currently `1.0`) — this repo does not use the gittle extension or
  `${revision}`, unlike sibling projects in `~/src/emergent`.
- Lombok (`@Value`, `@Data`) is used for value/config classes; `lombok.config` and
  `.editorconfig` define formatting.
- Maven runtime artifacts (maven-core, maven-model, plugin-api, resolver-api, …) must stay
  `provided`; `plexus-utils` stays `compile` scope (MNG-6965).
