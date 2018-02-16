package org.jabref.support;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class CIServerCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("not on CI server");

    private static boolean isCIServer() {
        // See http://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
        // See https://circleci.com/docs/environment-variables
        return Boolean.valueOf(System.getenv("CI"));
    }

    /**
     * Containers and tests are disabled if they are annotated with {@link DisabledOnCIServer} and they tests are run on
     * the CI server.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!isCIServer()) {
            return ENABLED;
        }

        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnCIServer> disabled = AnnotationUtils.findAnnotation(element, DisabledOnCIServer.class);
        if (disabled.isPresent()) {
            String reason = disabled.map(DisabledOnCIServer::value)
                    .filter(StringUtil::isNotBlank)
                    .orElseGet(() -> element.get() + " is disabled on CI server");
            return ConditionEvaluationResult.disabled(reason);
        }

        return ENABLED;
    }
}
