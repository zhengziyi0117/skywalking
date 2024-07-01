package org.apache.skywalking.oap.query.graphql.resolver;

import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.AsyncProfilerMutationService;
import org.apache.skywalking.oap.server.core.query.input.AsyncProfilerTaskCreationRequest;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTaskCreationResult;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

@Slf4j
public class AsyncProfilerQuery implements GraphQLQueryResolver {
    private final ModuleManager moduleManager;

    private AsyncProfilerMutationService queryService;

    public AsyncProfilerQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public AsyncProfilerTaskCreationResult virtualQuery(AsyncProfilerTaskCreationRequest profilerTaskCreationRequest) {
        return new AsyncProfilerTaskCreationResult("test interface", "123");
    }
}
