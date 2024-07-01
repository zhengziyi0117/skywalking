package org.apache.skywalking.oap.server.core.query.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum AsyncProfilerEventType {
    CPU(0, "cpu"),
    WALL(1, "wall"),
    LOCK(2, "lock"),
    ALLOC(3, "alloc");

    private final int code;
    private final String name;

    public static List<AsyncProfilerEventType> valueOfList(List<String> events) {
        return events.stream().map(AsyncProfilerEventType::valueOf)
                .collect(Collectors.toList());
    }
}
