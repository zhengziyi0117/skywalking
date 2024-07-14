package org.apache.skywalking.oap.server.core.query.input;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerDataFormatType;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerEventType;

import java.util.List;

@Getter
@Setter
public class AsyncProfilerTaskCreationRequest {
    private String serviceId;
    private String serviceInstanceId;
    private int duration;
    private AsyncProfilerDataFormatType dataFormat;
    private List<AsyncProfilerEventType> events;
    private String execArgs;
}
