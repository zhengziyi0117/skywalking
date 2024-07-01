package org.apache.skywalking.oap.server.core.source;

import lombok.Data;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.JFREventType;

import java.util.List;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.JFR_PROFILING_DATA;


@Data
@ScopeDeclaration(id = JFR_PROFILING_DATA, name = "JFRProfilingData")
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class JFRProfilingData extends Source {
    private volatile String entityId;

    @Override
    public int scope() {
        return JFR_PROFILING_DATA;
    }

    @Override
    public String getEntityId() {
        if (entityId == null) {
            return taskId;
        }
        return entityId;
    }

    private String taskId;
    private long startTime;
    private long interval;
    private JFREventType eventType;
    private List<String> stackFrames;

    private long duration;

    private long tlabSize;
}
