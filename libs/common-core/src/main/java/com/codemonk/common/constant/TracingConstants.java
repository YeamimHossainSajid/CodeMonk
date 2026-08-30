package com.codemonk.common.constant;

public final class TracingConstants {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_TRACE_ID = "traceId";

    private TracingConstants() {
    }
}
