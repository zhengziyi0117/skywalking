package org.apache.skywalking.oap.server.core.profiling.asyncprofiler;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.AsyncProfilerTaskLogRecord;
import org.apache.skywalking.oap.server.core.query.type.ProfileTaskLog;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskLogQueryDAO;
import org.apache.skywalking.oap.server.library.jfr.parser.convert.FrameTree;
import org.apache.skywalking.oap.server.library.jfr.parser.convert.JfrMergeBuilder;
import org.apache.skywalking.oap.server.library.jfr.parser.type.event.JfrEventType;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AsyncProfilerQueryService implements Service {
    private static final Gson GSON = new Gson();

    private final ModuleManager moduleManager;

    private IAsyncProfilerTaskQueryDAO taskQueryDAO;
    private IJfrDataQueryDAO dataQueryDAO;
    private IAsyncProfilerTaskLogQueryDAO logQueryDAO;

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
    private IAsyncProfilerTaskLogQueryDAO getAsyncProfilerTaskLogQueryDAO() {
        if (logQueryDAO == null) {
            this.logQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IAsyncProfilerTaskLogQueryDAO.class);
        }
        return logQueryDAO;
    }

    public List<AsyncProfilerTask> queryTask(String serviceId, Long startTime, Long endTime) throws IOException {
        Long startTimeBucket = null;
        if (Objects.nonNull(startTime)) {
            startTimeBucket = TimeBucket.getMinuteTimeBucket(startTime);
        }

        Long endTimeBucket = null;
        if (Objects.nonNull(endTime)) {
            endTimeBucket = TimeBucket.getMinuteTimeBucket(endTime);
        }

        return getAsyncProfileTaskDAO().getTaskList(serviceId, startTimeBucket, endTimeBucket, null);
    }

    public AsyncProfilerStackTree queryJfrData(String taskId, List<String> instanceIds, JfrEventType eventType) throws IOException {
        List<JfrProfilingDataRecord> jfrDataList = getJfrDataQueryDAO().getById(taskId, instanceIds, eventType.name());
        List<FrameTree> trees = jfrDataList.stream().map(data -> {
            return GSON.fromJson(new String(data.getDataBinary()), FrameTree.class);
        }).collect(Collectors.toList());
        FrameTree resultTree = new JfrMergeBuilder()
                .merge(trees)
                .build();
        return new AsyncProfilerStackTree(eventType, resultTree);
    }

    public List<ProfileTaskLog> queryAsyncProfilerTaskLogs(String taskId) throws IOException {
        // TODO
        Map<String, List<AsyncProfilerTaskLogRecord>> taskLogByTaskId = getAsyncProfilerTaskLogQueryDAO().getTaskLogByTaskId(Collections.singletonList(taskId));
        return taskLogByTaskId.get(taskId).stream().map((x)->new ProfileTaskLog()).collect(Collectors.toList());
    }
}

