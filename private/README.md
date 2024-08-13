# SLang Enterprise

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/slang-enterprise.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/slang-enterprise)
[![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=alert_status&token=sqb_8811ed1e8fa2ed1b4717cb3f2316ba0959aac1fe)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.slang%3Aslang)
[![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=coverage&token=sqb_8811ed1e8fa2ed1b4717cb3f2316ba0959aac1fe)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.slang%3Aslang)

## Building

Ensure you already have `artifactoryUsername` and `artifactoryPassword` set in your `~/.gradle/gradle.properties`
If not, look for `ARTIFACTORY_PRIVATE_USERNAME` and `ARTIFACTORY_PRIVATE_PASSWORD` values in the [Developer Box](https://xtranet-sonarsource.atlassian.net/wiki/spaces/DEV/pages/776711/Developer+Box#DeveloperBox-Gradlesettings) page.

Build and run Unit Tests:

    ./gradlew build

## Integration Tests

Update the submodules (do not limit to 'its/sources' like in the parent [README](../README.md#integration-tests)):

    git submodule update --init

Then build and run the Integration Tests using the 'its' property:

    ./gradlew build -Pits --info --no-daemon -Dsonar.runtimeVersion=7.4

To run only a subset of ITs, instead of `its` you can also use:
* `plugin`
* `ruling`
* `ruling-apex`
* `ruling-ruby`
* `ruling-scala`

To test private/its/ruling, without building the analyser plugins, but by downloading them from repox:

    ./gradlew :private:its:ruling:test -Pits --info --no-daemon -Dsonar.runtimeVersion=7.4 -DbuildNumber=92

Or using the full version number:

    ./gradlew :private:its:ruling:test -Pits --info --no-daemon -Dsonar.runtimeVersion=7.4 -DslangVersion=1.3.0.92

## Migrate an existing clone of SonarSource/slang to work with SonarSource/slang-enterprise
```bash
git checkout master
git remote rename origin slang
git remote add origin git@github.com:SonarSource/slang-enterprise.git
git fetch --all
git checkout origin/master
git branch --delete --force master
git checkout -b master origin/master
git branch master --set-upstream-to origin/master
git submodule update --init
```

## Importing an external PR from SonarSource/slang into SonarSource/slang-enterprise
```bash
# if you don't yet have a 'slang' remote
git remote add slang git@github.com:SonarSource/slang.git
# A user Paul push a PR #123 on SonarSource/slang, let's fetch it locally
git fetch slang pull/123/head:temporary-external-pr-branch
# List the external PR commits and remember the list of SHA1 from Paul for the following "git cherry-pick"
git log -10 --pretty="format:%h [%an] %s" temporary-external-pr-branch
# your can delete the temporary branch
git branch -D temporary-external-pr-branch
# Create a branch "SONARSLANG-456" based on origin/master to import the external PR
git fetch origin
git checkout -b SONARSLANG-456 origin/master
# cherry-pick on SONARSLANG-456 branch Paul's commits previously identified
git cherry-pick b06af3c2b06c8 ad4883ce577
```

## Fixing the synchronization from `SonarSource/slang-enterprise` to `SonarSource/slang`

When there's a failing [public-sync](https://github.com/SonarSource/slang-enterprise/actions/workflows/public-sync.yml) GitHub Action you can
fix the problem with those commands:

> It comes from [BUILD-4012 Fix inconsistent state in synchronization script for public_master branch](https://sonarsource.atlassian.net/browse/BUILD-4012)

```shell
# Go to "slang-enterprise" or clone it
$ git clone git@github.com:SonarSource/slang-enterprise.git
$ cd slang-enterprise

# Add the public repository "SonarSource/slang" as a remote called "slang"
$ git remote add slang git@github.com:SonarSource/slang.git

$ git remote -v
origin	git@github.com:SonarSource/slang-enterprise.git (fetch)
origin	git@github.com:SonarSource/slang-enterprise.git (push)
slang	git@github.com:SonarSource/slang.git (fetch)
slang	git@github.com:SonarSource/slang.git (push)

# Refresh both remotes references
$ git fetch origin
$ git fetch slang

# Download the previous references used by the synchronization action 
$ git fetch --no-tags origin "+refs/public_sync/*:refs/public_sync/*"
...
 * [new ref]           refs/public_sync/2023-09-28_00-14-15/master        -> refs/public_sync/2023-09-28_00-14-15/master
 * [new ref]           refs/public_sync/2023-09-28_00-14-15/public_master -> refs/public_sync/2023-09-28_00-14-15/public_master
 * [new ref]           refs/public_sync/2023-10-03_00-14-28/master        -> refs/public_sync/2023-10-03_00-14-28/master
 * [new ref]           refs/public_sync/2023-10-03_00-14-28/public_master -> refs/public_sync/2023-10-03_00-14-28/public_master

# List the 7 last commits on the "SonarSource/slang-enterprise" private side
$ git log --graph --pretty=format:'%H %d %s' origin/master -7
* c568726b297569ab0fae6ac52c9fa94f43d0807f  (HEAD -> master, origin/master, origin/HEAD) Upgrade gradle version to 7.6.3 (#507)
* d9ba8c8b5f7000bb3d6b979739ec089913f364b9  Prepare for next development iteration 16-SNAPSHOT (#505)
* 28938525c4093d5e9fa4e858535bbcb60af7f066  (tag: 1.15.0.4655) Downgrade analyzer-commons to the latest released version (#504)
* 1dd91acb40c480d81cae1147c3d86ad906691625  SONARSLANG-632 Update rules metadata Apex
* ee5311163f9aac75792d124acf04ef7e8f07cb48  SONARSLANG-632 Update rules metadata for Go, Ruby and Scala
* 63beff6a2a540e0cc2445a4696ae7cf899dd76a4  Upgrade dependencies (#502)
* 086e0b464ebee2e9df2491bf8e6015837d4cbe39  SONARSLANG-613 enable NOSONAR in SonarLint (#497)

# List the 3 commits on the "SonarSource/slang" public side
$ git log --graph --pretty=format:'%H %d %s' slang/master -3
* 6798435cea0a9ad2cae78ad8a8bc34a86c11757a  (slang/master) SONARSLANG-632 Update rules metadata for Go, Ruby and Scala (#328)
* b35c9069d8780681001cc72489a47420e4066854  (origin/public_master) SONARSLANG-632 Update rules metadata for Go, Ruby and Scala
* 594a311c2741bcd0820cdb3dd8f88460c5f91234  Upgrade dependencies (#502)

# Here we see that the "6798435cea0a9ad2cae78ad8a8bc34a86c11757a" commit is the problem.
# It's commit created on "SonarSource/slang" instead of "SonarSource/slang-enterprise"
# Even, if the commit is empty, it brakes the synchronization 

# Find the commits that we should consider as safe to continue the synchronization after:
# On origin/master side: ee5311163f9aac75792d124acf04ef7e8f07cb48  SONARSLANG-632 Update rules metadata for Go, Ruby and Scala
# On slang/master  side: 6798435cea0a9ad2cae78ad8a8bc34a86c11757a  SONARSLANG-632 Update rules metadata for Go, Ruby and Scala (#328)

# Create two new references using the current GTM time to save the safe commits.
# Note: On "SonarSource/slang" side (public_master) it should match the branch slang/master.
$ git update-ref refs/public_sync/2023-10-19_07-20-00/master ee5311163f9aac75792d124acf04ef7e8f07cb48
$ git update-ref refs/public_sync/2023-10-19_07-20-00/public_master 6798435cea0a9ad2cae78ad8a8bc34a86c11757a
$ git push origin refs/public_sync/2023-10-19_07-20-00/master
$ git push origin refs/public_sync/2023-10-19_07-20-00/public_master

# And finally, manually run the "public-sync" GitHub action with the "Run Workflow" button
# at https://github.com/SonarSource/slang-enterprise/actions/workflows/public-sync.yml
```

# Update Apex parser
1. Update parser installed in repox: follow instructions in [create-apex-jorje-lsp](https://github.com/SonarSource/slang-enterprise/tree/master/private/create-apex-jorje-lsp)
2. Update dependency version in `build.gradle`
3. Run `AstVisitorCodeGenerator::main` and `GeneratedApexAstVisitorTest::main`. To do so, you may need to delete `GeneratedApexAstVisitor` first. 
 

# Code Quality and Security for Apex

This SonarSource project is a [static code analyser](https://en.wikipedia.org/wiki/Static_program_analysis) for the Salesforce's Apex language.

Project homepage:
http://docs.sonarqube.org/display/PLUG/SonarApex

Issue tracking:
https://jira.sonarsource.com/browse/SONARSLANG
