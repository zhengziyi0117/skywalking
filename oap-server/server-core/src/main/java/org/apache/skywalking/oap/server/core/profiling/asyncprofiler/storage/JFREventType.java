package org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage;

public enum JFREventType {
    UNKNOWN,
    EXECUTION_SAMPLE,
    JAVA_MONITOR_ENTER,
    THREAD_PARK,
    OBJECT_ALLOCATION_IN_NEW_TLAB,
    OBJECT_ALLOCATION_OUTSIDE_TLAB;
}
