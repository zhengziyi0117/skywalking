package org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler;

import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.storage.DAO;

import java.io.IOException;
import java.util.List;

public interface IAsyncProfilerTaskQueryDAO extends DAO {
    /**
     * search task list in appoint time bucket
     *
     * @param serviceInstanceId       monitor service instance id, maybe null
     * @param startTimeBucket time bucket bigger than or equals, nullable
     * @param endTimeBucket   time bucket small than or equals, nullable
     * @param limit           limit count, if null means query all
     */
    List<AsyncProfilerTask> getTaskList(final String serviceInstanceId, final Long startTimeBucket,
                                        final Long endTimeBucket, final Integer limit) throws IOException;

    /**
     * query profile task by id
     */
    AsyncProfilerTask getById(final String id) throws IOException;
}
