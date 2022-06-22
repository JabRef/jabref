# See https://www.gitpod.io/docs/java-in-gitpod/ for a full documentation of Java in GitPod

FROM gitpod/workspace-full

# All available versions can be listed using sdk ls java
# More information about SDKMAN available at https://github.com/sdkman/sdkman-cli#sdkman-cli
RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java 18.0.1.1-open)"
