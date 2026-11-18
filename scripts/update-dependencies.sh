#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Refreshes the Gradle dependency lockfiles and gradle/verification-metadata.xml
# (SHA-256 of every artifact). Run it after any dependency or plugin version
# change, for example on each Gradle pull request opened by Dependabot:
#
#   git checkout <dependabot-branch> && ./scripts/update-dependencies.sh
#   git commit -am "Update dependency locks and verification metadata" && git push
#
# Review the diff before committing: new checksums should only appear for the
# versions the pull request changes.
# -----------------------------------------------------------------------------
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INIT_SCRIPT="$(mktemp -t resolve-all-XXXXXX).gradle.kts"
trap 'rm -f "$INIT_SCRIPT"' EXIT

# Downloads the artifacts of every configuration, including those only resolved when a task runs
# (for example the AspectJ agent used by the E2E tasks), so all of them get a checksum.
cat > "$INIT_SCRIPT" <<'EOF'
allprojects {
    tasks.register("resolveAllArtifacts") {
        notCompatibleWithConfigurationCache("resolves every configuration")
        doLast {
            (project.configurations + project.buildscript.configurations)
                .filter { it.isCanBeResolved }
                .forEach { it.incoming.artifactView { lenient(true) }.files.files }
        }
    }
}
EOF

cd "$ROOT_DIR"
# A single invocation: --write-locks resolves the graph without the lock constraints, which reads extra metadata
# (for example the POMs of intermediate versions), and that metadata needs a checksum too.
# --refresh-dependencies re-reads metadata from the repositories, so files already in the local cache
# (for example Gradle module metadata) also get a checksum.
./gradlew --no-configuration-cache --refresh-dependencies --init-script "$INIT_SCRIPT" \
  --write-locks --write-verification-metadata sha256 \
  dependencies buildEnvironment build sonarResolver resolveAllArtifacts --quiet > /dev/null

git --no-pager diff --stat -- '*.lockfile' gradle/verification-metadata.xml
