# Relocator

Relocator is a Maven plugin that replaces specific dependencies with substitutes during the build.
Conceptually, it works like Maven's [dependency relocation](https://maven.apache.org/guides/mini/guide-relocation.html)
mechanism, but the replacements are driven entirely by plugin configuration — no specially prepared
relocation POMs need to be published to a repository.

Typical uses: swapping `log4j:log4j` for `ch.qos.reload4j:reload4j`, migrating off renamed
artifacts, or forcing a fork of a library in place of the original — including when the unwanted
artifact only arrives *transitively* through third-party dependencies.

## Usage

Declare the plugin with `<extensions>true</extensions>` and configure replacement rules. That's
all: replacement happens transparently on every build, before dependency resolution.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.emergent.maven.plugins</groupId>
      <artifactId>relocator-maven-plugin</artifactId>
      <version>1.0</version>
      <extensions>true</extensions>
      <configuration>
        <replacements>
          <replacement>
            <source>log4j:log4j</source>
            <target>ch.qos.reload4j:reload4j:1.2.26</target>
          </replacement>
        </replacements>
      </configuration>
    </plugin>
  </plugins>
</build>
```

`<extensions>true</extensions>` is required — it is what allows the plugin to hook the build early
enough (after project models are read, before any resolution) to rewrite dependencies for the whole
build, mojo executions, and all. Without it only the diagnostic goals are available.

## Replacement rules

Each `<replacement>` has two elements:

| Element    | Format                                                                              | Meaning                                                                                                                                                                                                                                                                         |
|------------|-------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<source>` | `groupId:artifactId`, `groupId:artifactId:version`, or `groupId:artifactId:[range]` | What to match. GA match is exact. A plain version matches only that exact version; a [version range](https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html) such as `[1.0,2.0)` matches versions inside the range. Without a version part, every version matches. |
| `<target>` | `groupId:artifactId:version`                                                        | The full coordinates of the replacement. The version is required.                                                                                                                                                                                                               |

## How it works

At `afterProjectsRead` (before anything is resolved), for every project the plugin is configured on:

1. **Direct dependencies** declared in the POM that match a rule are rewritten in the in-memory
   model to the replacement coordinates. Scope, type, classifier, optionality, and exclusions are
   preserved.
2. **Transitive dependencies** are found by collecting the project's dependency graph (POM metadata
   only, no jar downloads). For each rule matched somewhere below a direct dependency, the plugin
   adds an `<exclusion>` for the matched artifact to every direct dependency that carries it and
   injects the replacement as a direct dependency, using the widest scope in which the matched
   artifact occurred. If the replacement is already a (possibly just-rewritten) direct dependency,
   only the exclusions are added.

Every applied replacement is logged at info level, prefixed with `relocator:`.

### Caveats

- Replacements affect **this build's** resolution and classpaths. The installed/deployed POM is the
  original file on disk, so consumers of a published artifact do not inherit the replacements —
  unlike true repository relocation, which acts at the consumer's resolution time.
- Maven exclusions are version-blind. When a version-ranged rule matches a transitive artifact, the
  whole groupId:artifactId is excluded from the carrying dependencies. When the range does *not*
  match the resolved version, the rule is a no-op for that occurrence.
- Dependencies of *plugins*, and entries in `<dependencyManagement>`, are not rewritten.

## Goals

Goal prefix: `relocator`.

- `relocator:list` — prints the configured replacement rules and, when the extension is active, the
  replacements applied to the current project.
- `relocator:help` — help for the plugin's goals.

## Disabling

Pass `-Drelocator.skip=true` (or set the environment variable `RELOCATOR_SKIP=true`) to skip replacement for a build.

## Building

Java 17 and the Maven wrapper:

```bash
./mvnw verify              # build + unit tests
./mvnw verify -Prun-its    # plus end-to-end integration tests (maven-invoker-plugin)
```

Integration test scenarios live under `src/it/`; each is a self-contained project with a
`verify.groovy` asserting on the resolved dependency list.
