#!/bin/bash
handle_sigint() {
    echo "Caught SIGINT, executing additional code..."
    # Your additional code here
    ./terminateChunkserver.sh
    exit 1
}

# Set up the trap
trap 'handle_sigint' SIGINT

./terminateChunkserver.sh


command1="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver1 cluster 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 0"

command2="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver2 cluster 127.0.0.1:8082 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 1"

command3="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver3 cluster 127.0.0.1:8083 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 2"

command4="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver4 cluster 127.0.0.1:8084 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 3"

command5="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver5 cluster 127.0.0.1:8085 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 4"

command6="/usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver chunkserver6 cluster 127.0.0.1:8086 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 5"

# Run commands concurrently
$command1 &
$command2 &
$command3 &
$command4 &
$command5 &
$command6 &

# Wait for all commands to finish
wait