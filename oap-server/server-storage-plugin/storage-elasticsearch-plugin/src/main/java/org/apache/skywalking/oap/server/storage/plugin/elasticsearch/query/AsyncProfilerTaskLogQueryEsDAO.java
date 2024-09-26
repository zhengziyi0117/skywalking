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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.query;

import org.apache.skywalking.library.elasticsearch.requests.search.BoolQueryBuilder;
import org.apache.skywalking.library.elasticsearch.requests.search.Query;
import org.apache.skywalking.library.elasticsearch.requests.search.Search;
import org.apache.skywalking.library.elasticsearch.requests.search.SearchBuilder;
import org.apache.skywalking.library.elasticsearch.response.search.SearchHit;
import org.apache.skywalking.library.elasticsearch.response.search.SearchResponse;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.AsyncProfilerTaskLogRecord;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskLogQueryDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.ElasticSearchConverter;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.IndexController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsyncProfilerTaskLogQueryEsDAO extends EsDAO implements IAsyncProfilerTaskLogQueryDAO {

    public AsyncProfilerTaskLogQueryEsDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override
    public Map<String, List<AsyncProfilerTaskLogRecord>> getTaskLogByTaskId(List<String> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return null;
        }
        final String index = IndexController.LogicIndicesRegister.getPhysicalTableName(AsyncProfilerTaskLogRecord.INDEX_NAME);

        final BoolQueryBuilder query = Query.bool();
        if (IndexController.LogicIndicesRegister.isMergedTable(AsyncProfilerTaskLogRecord.INDEX_NAME)) {
            query.must(Query.term(IndexController.LogicIndicesRegister.RECORD_TABLE_NAME, AsyncProfilerTaskLogRecord.INDEX_NAME));
        }
        query.must(Query.terms(AsyncProfilerTaskLogRecord.TASK_ID, taskIds));

        final SearchBuilder search = Search.builder().query(query);
        final SearchResponse response = getClient().search(index, search.build());

        Map<String, List<AsyncProfilerTaskLogRecord>> taskId2Log = new HashMap<>();
        for (SearchHit searchHit : response.getHits().getHits()) {
            AsyncProfilerTaskLogRecord record = parseTaskLog(searchHit);
            taskId2Log.computeIfAbsent(record.getTaskId(), k -> new ArrayList<>()).add(record);
        }
        return taskId2Log;
    }

    private AsyncProfilerTaskLogRecord parseTaskLog(final SearchHit hit) {
        final Map<String, Object> sourceAsMap = hit.getSource();
        final AsyncProfilerTaskLogRecord.Builder builder = new AsyncProfilerTaskLogRecord.Builder();
        return builder.storage2Entity(new ElasticSearchConverter.ToEntity(AsyncProfilerTaskLogRecord.INDEX_NAME, sourceAsMap));
    }

}
