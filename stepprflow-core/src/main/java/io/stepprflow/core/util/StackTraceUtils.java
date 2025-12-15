package io.stepprflow.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for stack trace manipulation.
 */
public final class StackTraceUtils {

    /** Default maximum length for truncated stack traces. */
    public static final int DEFAULT_MAX_LENGTH = 2000;

    private StackTraceUtils() {
        // Utility class
    }

    /**
     * Truncates a stack trace to the default maximum length (2000 characters).
     *
     * @param throwable the exception to get stack trace from
     * @return truncated stack trace string, or empty string if throwable is null
     */
    public static String truncate(Throwable throwable) {
        return truncate(throwable, DEFAULT_MAX_LENGTH);
    }

    /**
     * Truncates a stack trace to the specified maximum length.
     * If the stack trace exceeds maxLength, it will be truncated and "..." appended.
     *
     * @param throwable the exception to get stack trace from
     * @param maxLength maximum length of the returned string (excluding "..." suffix)
     * @return truncated stack trace string, or empty string if throwable is null
     * @throws IllegalArgumentException if maxLength is negative
     */
    public static String truncate(Throwable throwable, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be non-negative");
        }

        if (throwable == null) {
            return "";
        }

        String stackTrace = getFullStackTrace(throwable);

        if (stackTrace.length() <= maxLength) {
            return stackTrace;
        }

        return stackTrace.substring(0, maxLength) + "...";
    }

    private static String getFullStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
