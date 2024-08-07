package org.apache.skywalking.oap.query.graphql.resolver;

import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.AsyncProfilerQueryService;
import org.apache.skywalking.oap.server.core.query.input.AsyncProfilerTaskRequest;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerAnalyzation;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerStackTree;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTaskResult;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import java.io.IOException;
import java.util.List;

@Slf4j
public class AsyncProfilerQuery implements GraphQLQueryResolver {
    private final ModuleManager moduleManager;

    private AsyncProfilerQueryService queryService;

    public AsyncProfilerQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private AsyncProfilerQueryService getAsyncProfilerQueryService() {
        if (queryService == null) {
            this.queryService = moduleManager.find(CoreModule.NAME)
                    .provider()
                    .getService(AsyncProfilerQueryService.class);
        }
        return queryService;
    }

    public AsyncProfilerTaskResult queryAsyncProfilerTask(AsyncProfilerTaskRequest request) throws IOException {
        List<AsyncProfilerTask> tasks = getAsyncProfilerQueryService().queryTask(
                request.getServiceInstanceId(), request.getStartTime(), request.getEndTime()
        );
        return new AsyncProfilerTaskResult(null, tasks);
    }

    public AsyncProfilerAnalyzation queryAsyncProfilerAnalyze(String taskId) throws IOException {
        List<AsyncProfilerStackTree> eventFrameTrees = getAsyncProfilerQueryService().queryJfrData(taskId);
        return new AsyncProfilerAnalyzation(null, eventFrameTrees);
    }
}
