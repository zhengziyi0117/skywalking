package org.apache.skywalking.oap.server.core.source;

import lombok.Data;
import one.convert.FrameTree;
import one.jfr.event.JfrEventType;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.JFR_PROFILING_DATA;


@Data
@ScopeDeclaration(id = JFR_PROFILING_DATA, name = "JfrProfilingData")
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class JfrProfilingData extends Source {
    private volatile String entityId;

    @Override
    public int scope() {
        return JFR_PROFILING_DATA;
    }

    @Override
    public String getEntityId() {
        if (entityId == null) {
            return taskId + uploadTime;
        }
        return entityId;
    }

    private String taskId;
    private long uploadTime;
    private JfrEventType eventType;
    private FrameTree frameTree;
}
