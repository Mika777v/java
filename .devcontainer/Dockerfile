# Base image of the codespace: Java 21 (Maven and Docker are added as features in devcontainer.json)
FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

# The image lists the Yarn package source, whose signing key can no longer be verified.
# Its error stops "apt-get update", so the Docker feature failed to install and the codespace
# started in recovery mode. Yarn is not used in this project, so the source is removed.
RUN rm -f /etc/apt/sources.list.d/yarn.list
