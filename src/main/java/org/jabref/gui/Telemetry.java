package org.jabref.gui;

import java.util.Optional;
import java.util.UUID;

import javafx.stage.Screen;

import org.jabref.logic.util.BuildInfo;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.TelemetryPreferences;

import com.google.common.base.StandardSystemProperty;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.SessionState;

public class Telemetry {
    private static TelemetryClient telemetryClient;

    private Telemetry() {
    }

    public static Optional<TelemetryClient> getTelemetryClient() {
        return Optional.ofNullable(telemetryClient);
    }

    private static void start(TelemetryPreferences telemetryPreferences, BuildInfo buildInfo) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
        if (!StringUtil.isNullOrEmpty(buildInfo.azureInstrumentationKey)) {
            telemetryConfiguration.setInstrumentationKey(buildInfo.azureInstrumentationKey);
        }
        telemetryConfiguration.setTrackingIsDisabled(!telemetryPreferences.shouldCollectTelemetry());
        telemetryClient = new TelemetryClient(telemetryConfiguration);
        telemetryClient.getContext().getProperties().put("JabRef version", buildInfo.version.toString());
        telemetryClient.getContext().getProperties().put("Java version", StandardSystemProperty.JAVA_VERSION.value());
        telemetryClient.getContext().getUser().setId(telemetryPreferences.getUserId());
        telemetryClient.getContext().getSession().setId(UUID.randomUUID().toString());
        telemetryClient.getContext().getDevice().setOperatingSystem(StandardSystemProperty.OS_NAME.value());
        telemetryClient.getContext().getDevice().setOperatingSystemVersion(StandardSystemProperty.OS_VERSION.value());
        telemetryClient.getContext().getDevice().setScreenResolution(Screen.getPrimary().getVisualBounds().toString());

        telemetryClient.trackSessionState(SessionState.Start);
    }

    public static void shutdown() {
        getTelemetryClient().ifPresent(client -> {
            client.trackSessionState(SessionState.End);
            client.flush();
        });
    }
}
