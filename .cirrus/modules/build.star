load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "gradle_signing_env",
    "pgp_signing_env",
    "next_env",
    "whitesource_api_env"
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master",
    "is_main_branch",
    "is_branch_qa_eligible"
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master",
    "base_image_container_builder",
    "custom_image_container_builder"
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master",
    "gradle_cache",
    "go_build_cache",
    "cleanup_gradle_script",
    "gradle_wrapper_cache",
    "project_version_cache",
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/actions.star@analysis/master",
    "default_gradle_on_failure"
)


#
# Build
#

def build_env():
    env = dict()
    env |= gradle_signing_env()
    env |= pgp_signing_env()
    env |= next_env()
    env |= {
        "DEPLOY_PULL_REQUEST": "true",
        "BUILD_ARGUMENTS": "--build-cache -x test -x sonar storeProjectVersion"
    }
    return env


def build_script():
    return [
        "git submodule update --init --depth 1 -- build-logic",
        "source cirrus-env BUILD-PRIVATE",
        "source .cirrus/use-gradle-wrapper.sh",
        "regular_gradle_build_deploy_analyze ${BUILD_ARGUMENTS}",
    ]


def build_task():
    return {
        "build_task": {
            "env": build_env(),
            "eks_container": custom_image_container_builder(dockerfile="Dockerfile", cpu=10, memory="6G"),
            "project_version_cache": project_version_cache(),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "go_build_cache": go_build_cache(go_src_dir="${CIRRUS_WORKING_DIR}/sonar-go-to-slang"),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": default_gradle_on_failure()
        }
    }


def build_test_sonar_env():
    return next_env() | {
        "DEPLOY_PULL_REQUEST": "false",
        "BUILD_ARGUMENTS": "--build-cache -x build -x artifactoryPublish test --configuration-cache"
    }

def go_and_gradle_on_failure():
    on_failure = default_gradle_on_failure()
    on_failure |= {
        "go_tests_artifacts": {
            "path": "**/build/test-report.json"
        }
    }
    return on_failure


def build_test_sonar_task():
    return {
        "build_test_sonar_task": {
            "env": build_test_sonar_env(),
            "depends_on": "build",
            "eks_container": custom_image_container_builder(dockerfile="Dockerfile", cpu=4, memory="8G"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "go_build_cache": go_build_cache(go_src_dir="${CIRRUS_WORKING_DIR}/sonar-go-to-slang"),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": go_and_gradle_on_failure()
        }
    }


#
# WhiteSource scan
#

def whitesource_script():
    return [
        "git submodule update --init --depth 1 -- build-logic",
        "source cirrus-env QA",
        "source .cirrus/use-gradle-wrapper.sh",
        "export PROJECT_VERSION=$(cat ${PROJECT_VERSION_CACHE_DIR}/evaluated_project_version.txt)",
        "GRADLE_OPTS=\"-Xmx64m -Dorg.gradle.jvmargs='-Xmx3G' -Dorg.gradle.daemon=false\" ./gradlew ${GRADLE_COMMON_FLAGS} :sonar-go-plugin:processResources -Pkotlin.compiler.execution.strategy=in-process",
        "source ws_scan.sh -d \"${PWD},${PWD}/sonar-go-to-slang\""
    ]


def sca_scan_task():
    return {
        "sca_scan_task": {
            "only_if": is_main_branch(),
            "depends_on": "build",
            "env": whitesource_api_env(),
            "eks_container": custom_image_container_builder(dockerfile="Dockerfile", cpu=1, memory="6G"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "go_build_cache": go_build_cache(go_src_dir="${CIRRUS_WORKING_DIR}/sonar-go-to-slang"),
            "project_version_cache": project_version_cache(),
            "whitesource_script": whitesource_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "allow_failures": "true",
            "always": {
                "ws_artifacts": {
                    "path": "whitesource/**/*"
                }
            },
        }
    }
