package com.wegongdu.rillway.ai.branch;

import com.wegongdu.rillway.core.context.ProcessContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts deterministic condition branch fingerprints from ProcessContext and prompts
 * to isolate cache slots between different business thresholds (e.g. leaveDays > 3 vs <= 3).
 */
public class BranchConditionExtractor {

    private static final Pattern THRESHOLD_PATTERN = Pattern.compile("(大于|超过|>|>=|小于|低于|<|<=)\\s*(\\d+(\\.\\d+)?)");

    /**
     * Computes a branch fingerprint string (e.g. "days>3", "amount<=5000", or "DEFAULT").
     */
    public static String computeBranchKey(String prompt, ProcessContext context) {
        if (context == null || prompt == null || prompt.isBlank()) {
            return "DEFAULT";
        }

        Map<String, Object> variables = context.variables();
        if (variables.isEmpty()) {
            return "DEFAULT";
        }

        // Search for relevant numeric variables in context (e.g. leaveDays, days, amount, count)
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String varName = entry.getKey();
            Object varValue = entry.getValue();

            if (varValue instanceof Number num) {
                BigDecimal actualValue = new BigDecimal(num.toString());

                // Find numeric threshold in prompt
                Matcher matcher = THRESHOLD_PATTERN.matcher(prompt);
                if (matcher.find()) {
                    String op = matcher.group(1);
                    BigDecimal threshold = new BigDecimal(matcher.group(2));

                    boolean isGreater = op.contains("大于") || op.contains("超过") || op.contains(">");
                    if (isGreater) {
                        if (actualValue.compareTo(threshold) > 0) {
                            return varName + ">" + threshold;
                        } else {
                            return varName + "<=" + threshold;
                        }
                    } else {
                        if (actualValue.compareTo(threshold) < 0) {
                            return varName + "<" + threshold;
                        } else {
                            return varName + ">=" + threshold;
                        }
                    }
                }
            }
        }

        return "DEFAULT";
    }
}
