load("github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master",
     "is_branch_qa_eligible",
     "are_changes_doc_only"
     )
load("github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master","go_env")
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master",
     "base_image_container_builder",
     "custom_image_container_builder",
     "arm64_container_builder",
     )
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master",
    "gradle_cache",
    "cleanup_gradle_script",
    "gradle_wrapper_cache",
    "go_build_cache",
    "orchestrator_cache",
    "set_orchestrator_home_script",
    "mkdir_orchestrator_home_script",
)
load("github.com/SonarSource/cirrus-modules/cloud-native/actions.star@analysis/master", "default_gradle_on_failure")

QA_PLUGIN_GRADLE_TASK = ":private:its:plugin:integrationTest"
QA_RULING_GRADLE_TASK = ":private:its:ruling:integrationTest"
QA_QUBE_LATEST_RELEASE = "LATEST_RELEASE"
QA_QUBE_DEV = "DEV"


def qa_task(env, run_its_script):
    return {
        "only_if": "({}) && !({})".format(is_branch_qa_eligible(), are_changes_doc_only()),
        "depends_on": "build",
        "eks_container": base_image_container_builder(cpu=4, memory="16G"),
        "env": env,
        "gradle_cache": gradle_cache(),
        "gradle_wrapper_cache": gradle_wrapper_cache(),
        "go_build_cache": go_build_cache(go_src_dir="${CIRRUS_WORKING_DIR}/sonar-go-to-slang"),
        "set_orchestrator_home_script": set_orchestrator_home_script(),
        "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
        "orchestrator_cache": orchestrator_cache(),
        "run_its_script": run_its_script,
        "on_failure": default_gradle_on_failure(),
        "cleanup_gradle_script": cleanup_gradle_script()
    }


def qa_plugin_env():
    return {
        "GRADLE_TASK": QA_PLUGIN_GRADLE_TASK,
        "KEEP_ORCHESTRATOR_RUNNING": "true",
        "matrix": [
            {"SQ_VERSION": QA_QUBE_LATEST_RELEASE},
            {"SQ_VERSION": QA_QUBE_DEV},
        ],
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }


def qa_script():
    return [
        "git submodule update --init --depth 1",
        "source cirrus-env QA",
        "source .cirrus/use-gradle-wrapper.sh",
        "./gradlew \"${GRADLE_TASK}\" \"-Dsonar.runtimeVersion=${SQ_VERSION}\" --info --build-cache --console plain --no-daemon --configuration-cache"
    ]


def qa_plugin_task():
    return {
        "qa_plugin_task": qa_task(qa_plugin_env(), qa_script())
    }


def qa_ruling_env():
    return {
        "GRADLE_TASK": QA_RULING_GRADLE_TASK,
        "KEEP_ORCHESTRATOR_RUNNING": "true",
        "SQ_VERSION": QA_QUBE_LATEST_RELEASE,
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }


def qa_ruling_task():
    return {
        "qa_ruling_task": qa_task(qa_ruling_env(), qa_script())
    }


def qa_arm64_condition():
    return "$CIRRUS_PR_LABELS =~ \".*qa-arm64.*\" || $CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\""


def qa_arm64_env():
    return go_env() | {
        "SQ_VERSION": QA_QUBE_LATEST_RELEASE,
        "GO_CROSS_COMPILE": "0",
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }


def qa_arm64_task():
    return {
        "qa_arm64_task": {
            "depends_on": "build",
            "only_if": "({}) && !({})".format(qa_arm64_condition(), are_changes_doc_only()),
            "env": qa_arm64_env(),
            "eks_container": arm64_container_builder(dockerfile="Dockerfile", cpu=4, memory="12G"),
            # In case Gradle cache contains platform-specific files, don't mix them
            "gradle_cache": gradle_cache(reupload_on_changes=False),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "go_build_cache": go_build_cache(go_src_dir="${CIRRUS_WORKING_DIR}/sonar-go-to-slang"),
            "set_orchestrator_home_script": set_orchestrator_home_script(),
            "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
            "orchestrator_cache": orchestrator_cache(),
            "run_script": [
                "git submodule update --init --depth 1",
                "source cirrus-env QA",
                "./gradlew test :private:its:ruling:integrationTest \"-Dsonar.runtimeVersion=${SQ_VERSION}\" --info --console plain --configuration-cache"
            ],
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": default_gradle_on_failure()
        }
    }
