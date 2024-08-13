#!/usr/bin/env bash

export APEX_CODE_EDITOR_VERSION='58.14.2'
export APEX_JORJE_LSP_URL="https://github.com/forcedotcom/salesforcedx-vscode/raw/v${APEX_CODE_EDITOR_VERSION}/packages/salesforcedx-vscode-apex/out/apex-jorje-lsp.jar"
export APEX_JORJE_LSP_JAR="target/apex-jorje-lsp-${APEX_CODE_EDITOR_VERSION}.jar"
export APEX_JORJE_LSP_MINIMIZED_JAR="target/apex-jorje-lsp-minimized-${APEX_CODE_EDITOR_VERSION}.jar"

mkdir -p target/META-INF

echo "== Downloading ${APEX_JORJE_LSP_URL} into ${APEX_JORJE_LSP_JAR} =="
curl -L -o "${APEX_JORJE_LSP_JAR}" "${APEX_JORJE_LSP_URL}"
du -h "${APEX_JORJE_LSP_JAR}"

cat << EOF > target/META-INF/MANIFEST.MF
Manifest-Version: 1.0
Implementation-Title: apex-jorje-lsp-minimized
Implementation-Version: 218.0-SNAPSHOT
Specification-Title: apex-jorje-lsp-minimized
Implementation-Vendor-Id: com.salesforce.apex
Build-Jdk: 1.8.0_172
Specification-Version: 218.0-SNAPSHOT
EOF

INCLUDED="META-INF/MANIFEST.MF|META-INF/LICENSE"
INCLUDED="$INCLUDED|apex/jorje/data/|apex/jorje/parser/|apex/jorje/services/|apex/common/"
INCLUDED="$INCLUDED|org/antlr/|antlr/"
INCLUDED="$INCLUDED|com/google/common/cache/|com/google/common/base/|com/google/common/util/"
INCLUDED="$INCLUDED|com/google/common/collect/|com/google/common/math/|com/google/common/primitives/"

cp "${APEX_JORJE_LSP_JAR}" "${APEX_JORJE_LSP_MINIMIZED_JAR}" && \
jar tf "${APEX_JORJE_LSP_JAR}" | \
  egrep -v "^($INCLUDED)" | \
  xargs zip -d "${APEX_JORJE_LSP_MINIMIZED_JAR}"

(cd target && zip -u "../${APEX_JORJE_LSP_MINIMIZED_JAR}" META-INF/MANIFEST.MF)

echo
du -h "${APEX_JORJE_LSP_JAR}"
du -h "${APEX_JORJE_LSP_MINIMIZED_JAR}"

echo mvn install:install-file -Dfile="${APEX_JORJE_LSP_MINIMIZED_JAR}" \
  -DgroupId=com.salesforce \
  -DartifactId=apex-jorje-lsp-minimized \
  -Dversion="${APEX_CODE_EDITOR_VERSION}" \
  -Dpackaging=jar

