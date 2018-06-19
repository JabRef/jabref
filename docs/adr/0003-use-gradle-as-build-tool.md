# Use Gradle as build tool

## Context and Problem Statement

Which build tool should be used?

## Considered Options

* [Maven](https://maven.apache.org/)
* [Gradle](https://gradle.org/)
* [Ant](https://ant.apache.org/)

## Decision Outcome

Chosen option: "Gradle", because it is lean and fits our development style.

## Pros and Cons of the Options

### Maven

* Good, because [there is a plugin for almost everything](https://www.slant.co/versus/2107/11592/~apache-maven_vs_gradle)
* Good, because [it has good integration with third party tools](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Good, because [it has robust performance](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Good, because [it has a high popularity](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Good, [if one favors declarative over imperative](https://www.slant.co/versus/2107/11592/~apache-maven_vs_gradle)
* Bad, because [getting a dependency list is not straight forward](https://stackoverflow.com/q/1677473/873282)
* Bad, because [it based on a fixed and linear model of phases](https://dzone.com/articles/gradle-vs-maven)
* Bad, because [it is hard to customize](https://www.slant.co/versus/2107/11592/~apache-maven_vs_gradle)
* Bad, because [it needs plugins for everything](https://www.slant.co/versus/2107/11592/~apache-maven_vs_gradle)
* Bad, because [it is verbose leading to huge build files](https://technologyconversations.com/2014/06/18/build-tools/)

### Gradle

* Good, because [its build scripts are short](https://technologyconversations.com/2014/06/18/build-tools/)
* Good, because [it follows the convention over configuration approach](https://www.safaribooksonline.com/library/view/building-and-testing/9781449306816/ch04.html)
* Good, because [it offers a graph-based task dependencies](https://dzone.com/articles/gradle-vs-maven)
* Good, because [it is easy to customize](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Good, because [it offers custom dependency scopes](https://gradle.org/maven-vs-gradle/)
* Good, because [it has good community support](https://linuxhint.com/ant-vs-maven-vs-gradle/)
* Good, because [its performance can be 100 times more than maven's performance](https://gradle.org/gradle-vs-maven-performance/).
* Bad, because [not that many plugins are available/maintained yet](https://blog.philipphauer.de/moving-back-from-gradle-to-maven/)
* Bad, because [it lacks a wide variety of application server integrations](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Bad, because [it has a medium popularity](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Bad, because [it allows custom build scripts which need to be debugged](https://www.softwareyoga.com/10-reasons-why-we-chose-maven-over-gradle/)

### Ant

* Good, because [it offers a lot of control over the build process](https://technologyconversations.com/2014/06/18/build-tools/)
* Good, because [it has an agile dependency manager](https://blog.alejandrocelaya.com/2014/02/22/dependency-management-in-java-projects-with-ant-and-ivy/)
* Good, because [it has a low learning curve](https://technologyconversations.com/2014/06/18/build-tools/)
* Bad, because [build scripts can quickly become huge](https://technologyconversations.com/2014/06/18/build-tools/)
* Bad, because [everything has to be written from scratch](http://www.baeldung.com/ant-maven-gradle)
* Bad, because [no conventions are enforced which can make it hard to understand someone else's build script](http://www.baeldung.com/ant-maven-gradle)
* Bad, because [it has nearly no community support](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Bad, because [it has a low popularity](http://pages.zeroturnaround.com/rs/zeroturnaround/images/java-build-tools-part-2.pdf)
* Bad, because [it offers too much freedom](https://www.slant.co/versus/2106/2107/~apache-ant_vs_apache-maven)

## Links

* GADR: <https://github.com/adr/gadr-java/blob/master/gadr-java--build-tool.md>
