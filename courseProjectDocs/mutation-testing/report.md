A problem was found with the configuration of task ':pitest' (type 'PitestTask').
- In plugin 'info.solidsoft.pitest' type 'info.solidsoft.gradle.pitest.PitestTask' property 'targetClasses' doesn't have a configured value.

  Reason: This property isn't marked as optional and no value has been configured.

  Possible solutions:
    1. Assign a value to 'targetClasses'.
    2. Mark property 'targetClasses' as optional.

  For more information, please refer to https://docs.gradle.org/9.1.0/userguide/validation_problems.html#value_not_set in the Gradle documentation.

* Try:
> Assign a value to 'targetClasses'
> Mark property 'targetClasses' as optional
> Run with --scan to generate a Build Scan (Powered by Develocity).
