package org.apache.skywalking.oap.server.core.profiling.asyncprofiler;

import lombok.RequiredArgsConstructor;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.worker.NoneStreamProcessor;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.AsyncProfilerTaskRecord;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerDataFormatType;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerEventType;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTaskCreationResult;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.library.util.StringUtil;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class AsyncProfilerMutationService implements Service {
    private final ModuleManager moduleManager;

    private IAsyncProfilerTaskQueryDAO taskQueryDAO;

    private IAsyncProfilerTaskQueryDAO getAsyncProfileTaskDAO() {
        if (taskQueryDAO == null) {
            this.taskQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IAsyncProfilerTaskQueryDAO.class);
        }
        return taskQueryDAO;
    }

    public AsyncProfilerTaskCreationResult createTask(String serviceId,
                                                      String serviceInstanceId,
                                                      int duration,
                                                      AsyncProfilerDataFormatType dataFormat,
                                                      List<AsyncProfilerEventType> events,
                                                      String execArgs) throws IOException {
        long createTime = System.currentTimeMillis();
        // check data
        final String errorMessage = checkDataSuccess(
                serviceId, serviceInstanceId, duration, createTime, dataFormat, events
        );
        if (errorMessage != null) {
            return AsyncProfilerTaskCreationResult.builder().errorReason(errorMessage).build();
        }

        // create task
        AsyncProfilerTaskRecord task = new AsyncProfilerTaskRecord();
        task.setTaskId(createTime + Const.ID_CONNECTOR + serviceInstanceId);
        task.setServiceId(serviceId);
        task.setServiceInstanceId(serviceId);
        task.setDuration(duration);
        task.setCreateTime(createTime);
        task.setExecArgs(execArgs);
        task.setTimeBucket(TimeBucket.getMinuteTimeBucket(createTime));
        NoneStreamProcessor.getInstance().in(task);

        return AsyncProfilerTaskCreationResult.builder().id(task.id().build()).build();
    }

    private String checkDataSuccess(String serviceId,
                                    String serviceInstanceId,
                                    long duration,
                                    long createTime,
                                    AsyncProfilerDataFormatType dataFormat,
                                    List<AsyncProfilerEventType> events) throws IOException {
        // basic check
        if (serviceId == null) {
            return "service cannot be null";
        }
        if (StringUtil.isEmpty(serviceInstanceId)) {
            return "serviceInstanceId name cannot be empty";
        }
        if (duration <= 0) {
            return "duration cannot be negative";
        }
        if (Objects.isNull(dataFormat)) {
            return "data format cannot be empty";
        }
        if (CollectionUtils.isEmpty(events)) {
            return "profile events cannot be empty";
        }

        // Each service can monitor up to 1 endpoints during the execution of tasks
        // TODO createTime+duration maybe hava bug
        long endTimeBucket = TimeBucket.getMinuteTimeBucket(createTime);
        final List<AsyncProfilerTask> alreadyHaveTaskList = getAsyncProfileTaskDAO().getTaskList(
                serviceId, null, endTimeBucket, 1
        );
        if (CollectionUtils.isNotEmpty(alreadyHaveTaskList)) {
            for (AsyncProfilerTask task : alreadyHaveTaskList) {
                if (task.getCreateTime() + TimeUnit.MINUTES.toMillis(task.getDuration()) >= createTime) {
                    // if the endTime is greater or equal than the startTime of the newly created task, i.e. there is overlap between two tasks, it is an invalid case
                    return "current service already has monitor async prorfiler task execute at this time";
                }
            }
        }
        return null;
    }
}
