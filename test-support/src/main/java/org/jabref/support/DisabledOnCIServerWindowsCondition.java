package org.jabref.support;

import java.util.Optional;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public class DisabledOnCIServerWindowsCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("Running not on Windows or not on CI server");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<DisabledOnCIServerWindows> annotation = context.getElement()
                                                                .flatMap(el -> AnnotationSupport.findAnnotation(el, DisabledOnCIServerWindows.class));

        if (annotation.isEmpty()) {
            return ENABLED;
        }

        boolean isOnCi = "true".equalsIgnoreCase(System.getenv("CI"));
        if (!isOnCi) {
            return ENABLED;
        }

        boolean isOnWindows = OS.WINDOWS.isCurrentOs();
        if (!isOnWindows) {
            return ENABLED;
        }

        return ConditionEvaluationResult.disabled(annotation.get().value());
    }
}
