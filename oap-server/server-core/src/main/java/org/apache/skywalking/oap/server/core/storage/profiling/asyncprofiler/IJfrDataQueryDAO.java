package org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler;

import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.JfrProfilingDataRecord;
import org.apache.skywalking.oap.server.library.module.Service;

import java.io.IOException;
import java.util.List;

public interface IJfrDataQueryDAO extends Service {
    /**
     * query profile task by id
     */
    List<JfrProfilingDataRecord> getById(final String id) throws IOException;
}
