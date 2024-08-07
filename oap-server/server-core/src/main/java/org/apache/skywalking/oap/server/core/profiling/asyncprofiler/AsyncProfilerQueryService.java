package org.apache.skywalking.oap.server.core.profiling.asyncprofiler;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import one.convert.FrameTree;
import one.jfr.event.JfrEventType;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.JfrProfilingDataRecord;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerStackTree;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IJfrDataQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AsyncProfilerQueryService implements Service {
    private static final Gson GSON = new Gson();

    private final ModuleManager moduleManager;

    private IAsyncProfilerTaskQueryDAO taskQueryDAO;
    private IJfrDataQueryDAO dataQueryDAO;

    private IAsyncProfilerTaskQueryDAO getAsyncProfileTaskDAO() {
        if (taskQueryDAO == null) {
            this.taskQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IAsyncProfilerTaskQueryDAO.class);
        }
        return taskQueryDAO;
    }

    private IJfrDataQueryDAO getJfrDataQueryDAO() {
        if (dataQueryDAO == null) {
            this.dataQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IJfrDataQueryDAO.class);
        }
        return dataQueryDAO;
    }

    public List<AsyncProfilerTask> queryTask(String serviceInstanceId, Long startTime, Long endTime) throws IOException {
        Long startTimeBucket = null;
        if (Objects.nonNull(startTime)) {
            startTimeBucket = TimeBucket.getMinuteTimeBucket(startTime);
        }

        Long endTimeBucket = null;
        if (Objects.nonNull(endTime)) {
            endTimeBucket = TimeBucket.getMinuteTimeBucket(endTime);
        }

        return getAsyncProfileTaskDAO().getTaskList(serviceInstanceId, startTimeBucket, endTimeBucket, null);
    }

    public List<AsyncProfilerStackTree> queryJfrData(String taskId) throws IOException {
        List<JfrProfilingDataRecord> jfrDataList = getJfrDataQueryDAO().getById(taskId);

        return jfrDataList.stream().map((data) -> {
            JfrEventType jfrEventType = JfrEventType.valueOf(data.getEventType());
            FrameTree frameTree = GSON.fromJson(new String(data.getDataBinary()), FrameTree.class);
            return new AsyncProfilerStackTree(jfrEventType, frameTree);
        }).collect(Collectors.toList());
    }
}
