load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/petertrr/support-develocity-config",
    "artifactory_env",
    "cirrus_env",
    "gradle_base_env",
    "gradle_develocity_env",
    "gradle_signing_env",
    "next_env",
    "gradle_env",
    "DEFAULT_GRADLE_FLAGS"
)


def project_version_env():
    return {
        "PROJECT_VERSION_CACHE_DIR": "project-version",
    }


def env():
    vars = artifactory_env()
    vars |= cirrus_env(depth=20)
    vars |= gradle_base_env()
    vars |= gradle_develocity_env()
    vars |= project_version_env()
    return {"env": vars}
