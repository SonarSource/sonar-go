# Creation of apex-jorje-lsp-minimized.jar

Analysis of Apex uses an Apex parser from a dependency `apex-jorje-lsp-minimized.jar`.
This dependency is generated using the `create-apex-jorje-lsp-minimized.sh` script and should be installed in repox.

For example, for the version '44.2.0' of salesforcedx-vscode, the original 17M jar is downloaded from:
https://github.com/forcedotcom/salesforcedx-vscode/raw/v44.2.0/packages/salesforcedx-vscode-apex/out/apex-jorje-lsp.jar

Then reduced to 4M into:
`target/apex-jorje-lsp-minimized-44.2.0.jar`

And builders installed it in repox:
https://repox.jfrog.io/repox/private-3rd-parties/com/salesforce/apex-jorje-lsp-minimized/44.2.0/apex-jorje-lsp-minimized-44.2.0.jar
