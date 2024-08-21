/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.skywalking.oap.server.core.CoreModuleConfig;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskLogQueryDAO;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AsyncProfilerTaskCache implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncProfilerTaskCache.class);

    private final Cache<String, AsyncProfilerTask> instanceId2taskCache;

    private final ModuleManager moduleManager;

    private IAsyncProfilerTaskQueryDAO taskQueryDAO;
    private IAsyncProfilerTaskLogQueryDAO taskLogQueryDAO;

    public AsyncProfilerTaskCache(ModuleManager moduleManager, CoreModuleConfig moduleConfig) {
        this.moduleManager = moduleManager;
        long initialSize = moduleConfig.getMaxSizeOfProfileTask() / 10L;
        int initialCapacitySize = (int) (initialSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : initialSize);

        instanceId2taskCache = CacheBuilder.newBuilder()
                .initialCapacity(initialCapacitySize)
                .maximumSize(moduleConfig.getMaxSizeOfProfileTask())
                // remove old profile task data
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    public IAsyncProfilerTaskLogQueryDAO getTaskLogQueryDAO() {
        if (Objects.isNull(taskLogQueryDAO)) {
            taskLogQueryDAO = moduleManager.find(StorageModule.NAME)
                                            .provider()
                                            .getService(IAsyncProfilerTaskLogQueryDAO.class);
        }
        return taskLogQueryDAO;
    }

    public IAsyncProfilerTaskQueryDAO getTaskQueryDAO() {
        if (Objects.isNull(taskQueryDAO)) {
            taskQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IAsyncProfilerTaskQueryDAO.class);
        }
        return taskQueryDAO;
    }

    public AsyncProfilerTask getAsyncProfilerTask(String serviceInstanceId) {
        return instanceId2taskCache.getIfPresent(serviceInstanceId);
    }

    public void saveTask(String serviceInstanceId, AsyncProfilerTask task) {
        if (task == null) {
            return ;
        }

        instanceId2taskCache.put(serviceInstanceId, task);
    }

    /**
     * use for every db query, -5 start time
     */
    public long getCacheStartTimeBucket() {
        return TimeBucket.getMinuteTimeBucket(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
    }

    /**
     * use for every db query, +5 end time(because use task start time to search)
     */
    public long getCacheEndTimeBucket() {
        return TimeBucket.getMinuteTimeBucket(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
    }
}
