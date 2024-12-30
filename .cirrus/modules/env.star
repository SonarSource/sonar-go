load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "artifactory_env",
    "cirrus_env",
    "gradle_signing_env",
    "next_env",
    "gradle_env",
    "DEFAULT_GRADLE_FLAGS"
)


def gradle_base_env():
    return {
        "GRADLE_USER_HOME": "${CIRRUS_WORKING_DIR}/.gradle",
        "GRADLE_COMMON_FLAGS": " ".join(DEFAULT_GRADLE_FLAGS)
    }


def gradle_develocity_env():
    return {
        "DEVELOCITY_TOKEN": "VAULT[development/kv/data/develocity data.token]",
        "DEVELOCITY_ACCESS_KEY": "develocity.sonar.build=${DEVELOCITY_TOKEN}"
    }


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
