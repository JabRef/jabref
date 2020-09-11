# See https://www.gitpod.io/docs/java-in-gitpod/ for a full documentation of Java in GitPod

FROM gitpod/workspace-full

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java 14.0.2-librca"
