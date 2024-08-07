package org.apache.skywalking.oap.server.core.query.input;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsyncProfilerTaskRequest {
    private String serviceId;
    private String serviceInstanceId;
    private Long startTime;
    private Long endTime;
}
