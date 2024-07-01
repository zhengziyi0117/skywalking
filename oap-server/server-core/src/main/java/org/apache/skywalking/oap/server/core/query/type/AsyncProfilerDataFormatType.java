package org.apache.skywalking.oap.server.core.query.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AsyncProfilerDataFormatType {
    JFR(0, "jfr"),
    HTML(1, "html");

    private final int code;
    private final String name;
}
