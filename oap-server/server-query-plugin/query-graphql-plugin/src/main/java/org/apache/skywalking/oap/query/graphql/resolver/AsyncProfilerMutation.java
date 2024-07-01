package org.apache.skywalking.oap.query.graphql.resolver;

import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.AsyncProfilerMutationService;
import org.apache.skywalking.oap.server.core.query.input.AsyncProfilerTaskCreationRequest;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTaskCreationResult;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import java.io.IOException;

@Slf4j
public class AsyncProfilerMutation implements GraphQLMutationResolver {
    private final ModuleManager moduleManager;

    private AsyncProfilerMutationService queryService;

    public AsyncProfilerMutation(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private AsyncProfilerMutationService getAsyncProfilerQueryService() {
        if (queryService == null) {
            this.queryService = moduleManager.find(CoreModule.NAME)
                    .provider()
                    .getService(AsyncProfilerMutationService.class);
        }
        return queryService;
    }

    public AsyncProfilerTaskCreationResult createAsyncProfilerTask(AsyncProfilerTaskCreationRequest request) throws IOException {
        AsyncProfilerMutationService asyncProfilerQueryService = getAsyncProfilerQueryService();
        return asyncProfilerQueryService.createTask(request.getServiceId(), request.getServiceInstanceId(),
                request.getDuration(), request.getDataFormat(), request.getEvents(), request.getExecArgs());
    }
}
