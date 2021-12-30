package org.jabref.gui.logging;

/*
 * ApplicationInsightsJava
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.microsoft.applicationinsights.internal.common.ApplicationInsightsEvent;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.tinylog.core.LogEntry;

// TODO: Remove this copy as soon as the one included in AI is compatible with log4j 3
public final class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

    private final LogEntry logEvent;

    public ApplicationInsightsLogEvent(LogEntry logEvent) {
        this.logEvent = logEvent;
    }

    @Override
    public String getMessage() {
        String message = this.logEvent.getMessage() != null ? this.logEvent.getMessage() : "Tinylog Trace";

        return message;
    }

    @Override
    public boolean isException() {
        return this.logEvent.getException() != null;
    }

    @Override
    public Exception getException() {
        Exception exception = null;

        if (isException()) {
            Throwable throwable = this.logEvent.getException();
            exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
        }

        return exception;
    }

    @Override
    public Map<String, String> getCustomParameters() {

        Map<String, String> metaData = new HashMap<>();

        metaData.put("SourceType", "slf4j");

        addLogEventProperty("LoggingLevel", logEvent.getLevel() != null ? logEvent.getLevel().name() : null, metaData);
        addLogEventProperty("ThreadName", logEvent.getThread().getName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(logEvent.getTimestamp().toInstant().toEpochMilli()), metaData);

        if (isException()) {
            addLogEventProperty("Logger Message", getMessage(), metaData);

            for (StackTraceElement stackTraceElement : logEvent.getException().getStackTrace()) {

                addLogEventProperty("ClassName", stackTraceElement.getClassName(), metaData);
                addLogEventProperty("FileName", stackTraceElement.getFileName(), metaData);
                addLogEventProperty("MethodName", stackTraceElement.getMethodName(), metaData);
                addLogEventProperty("LineNumber", String.valueOf(stackTraceElement.getLineNumber()), metaData);
            }

        }

        for (Entry<String, String> entry : logEvent.getContext().entrySet()) {
            addLogEventProperty(entry.getKey(), entry.getValue(), metaData);
        }

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.

        return metaData;
    }

    @Override
    public SeverityLevel getNormalizedSeverityLevel() {
        org.tinylog.Level logEventLevel = logEvent.getLevel();

        switch (logEventLevel) {

            case ERROR:
                return SeverityLevel.Error;
            case WARN:
                return SeverityLevel.Warning;
            case INFO:
                return SeverityLevel.Information;
            case TRACE:
            case DEBUG:
                return SeverityLevel.Verbose;
            default:
                InternalLogger.INSTANCE.error("Unknown slf4joption, %d, using TRACE level as default", logEventLevel);
                return SeverityLevel.Verbose;
        }
    }
}
