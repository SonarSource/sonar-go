load("github.com/SonarSource/cirrus-modules@v3", "load_features")
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/helper.star@analysis/master",
    "merge_dict"
)
load(".cirrus/modules/env.star", "env")
load(
    ".cirrus/modules/build.star",
    "build_task",
    "sca_scan_task"
)
load(
    ".cirrus/modules/qa.star",
    "qa_ruling_task",
    "qa_plugin_task",
)
load(".cirrus/modules/promote.star", "promote_task")


def main(ctx):
    conf = dict()
    merge_dict(conf, load_features(ctx))
    merge_dict(conf, env())
    merge_dict(conf, build_task())
    merge_dict(conf, qa_ruling_task())
    merge_dict(conf, qa_plugin_task())
    merge_dict(conf, sca_scan_task())
    merge_dict(conf, promote_task())
    return conf
