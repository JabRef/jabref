# Telemetry

JabRef aims for improving user experience. For that, it employs telemetry and, for instance, checks for used features.

The pull requests introducing the first version was [https://github.com/JabRef/jabref/pull/2283](https://github.com/JabRef/jabref/pull/2283). Self-hosted alternative [Matomo Java Tracker](https://github.com/matomo-org/matomo-java-tracker) where neglected, because the JabRef team currently does not have the resources to maintain a server.

## Implementation hints

The ApplicationInsights library that we use supports a special way to submit additional details: [https://docs.microsoft.com/en-us/azure/azure-monitor/app/api-custom-events-metrics#properties](https://docs.microsoft.com/en-us/azure/azure-monitor/app/api-custom-events-metrics#properties). Especially, one has to send `source` as property.
