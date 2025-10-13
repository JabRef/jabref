# Code coverage with sonarqube

We use <a href="https://sonarcloud.io">SonarCloud</a> for this

## How to use
1. Build the project for your device
2. Configure an api key in your shell for SonarQube
3. Use .gradlew sonar

## What needs implementation
- Currently, the deployment is done manually, however SonarQube has ci features -> it can be done with GitHub Workflows (and should)
