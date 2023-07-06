#!/bin/bash

# /usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Master.Master

mvn compile -X
command=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Master.Master")
"${command[@]}"