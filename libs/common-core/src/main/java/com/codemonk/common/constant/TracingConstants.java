package com.codemonk.common.constant;

/**
 * Common constants for distributed tracing, correlation headers, and logging MDC.
 */
public final class TracingConstants {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_TRACE_ID = "traceId";

    private TracingConstants() {
    }
}
