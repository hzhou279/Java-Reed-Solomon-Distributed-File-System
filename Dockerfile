# Start with a base image containing Java runtime
FROM openjdk:17-jdk-slim

# Add Maintainer Info
# LABEL maintainer="your_email@example.com"

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Install maven
RUN apt-get update && \
    apt-get install -y maven

# Compile the project
RUN mvn compile -X

CMD ["mvn", "exec:java", "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Master.Master"]