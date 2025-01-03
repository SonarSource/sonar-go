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
    "base_image_container_builder"
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/petertrr/support-develocity-config",
    "gradle_cache",
    "cleanup_gradle_script",
    "gradle_wrapper_cache",
    "project_version_cache",
    "store_project_version_script"
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
        "BUILD_ARGUMENTS": "--build-cache"
    }
    return env


def build_script():
    return [
        "git submodule update --init --depth 1 -- build-logic",
        "source cirrus-env BUILD",
        "source .cirrus/use-gradle-wrapper.sh",
        "regular_gradle_build_deploy_analyze ${BUILD_ARGUMENTS}",
        "source set_gradle_build_version ${BUILD_NUMBER}",
        "echo export PROJECT_VERSION=${PROJECT_VERSION} >> ~/.profile"
    ]


def build_task():
    return {
        "build_task": {
            "env": build_env(),
            "eks_container": base_image_container_builder(cpu=4, memory="6G"),
            "project_version_cache": project_version_cache(),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "store_project_version_script": store_project_version_script(),
            "on_failure": default_gradle_on_failure()
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
        "source ${PROJECT_VERSION_CACHE_DIR}/evaluated_project_version.txt",
        "GRADLE_OPTS=\"-Xmx64m -Dorg.gradle.jvmargs='-Xmx3G' -Dorg.gradle.daemon=false\" ./gradlew ${GRADLE_COMMON_FLAGS} :sonar-go-plugin:processResources -Pkotlin.compiler.execution.strategy=in-process",
        "source ws_scan.sh -d \"${PWD},${PWD}/sonar-go-to-slang\""
    ]


def sca_scan_task():
    return {
        "sca_scan_task": {
            "only_if": is_main_branch(),
            "depends_on": "build",
            "env": whitesource_api_env(),
            "eks_container": base_image_container_builder(cpu=1, memory="4G"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
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
