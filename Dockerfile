# Start with a base image containing Java runtime
FROM openjdk:17-jdk-slim

# Add Maintainer Info
# LABEL maintainer="your_email@example.com"

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

RUN chmod +x wait-for-master.sh

# Install netcat
RUN apt-get update && apt-get install -y netcat

# Install maven
RUN apt-get update && \
    apt-get install -y maven


# Install Docker-Cli
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo \
    "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && apt-get install -y docker-ce-cli

# Install docker-compose
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

# Install vim
RUN apt-get update && apt-get install -y vim

# Compile the project
RUN mvn clean compile -X

CMD ["mvn", "exec:java", "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Master.Master"]