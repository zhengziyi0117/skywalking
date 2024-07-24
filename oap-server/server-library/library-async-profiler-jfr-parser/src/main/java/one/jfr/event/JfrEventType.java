package one.jfr.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JfrEventType {
    UNKNOWN(-1),
    EXECUTION_SAMPLE(1),
    JAVA_MONITOR_ENTER(2),
    THREAD_PARK(3),
    OBJECT_ALLOCATION_IN_NEW_TLAB(4),
    OBJECT_ALLOCATION_OUTSIDE_TLAB(5),
    PROFILER_LIVE_OBJECT(6);

    private final int code;
}
