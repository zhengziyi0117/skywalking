package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.query;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.skywalking.library.elasticsearch.requests.search.BoolQueryBuilder;
import org.apache.skywalking.library.elasticsearch.requests.search.Query;
import org.apache.skywalking.library.elasticsearch.requests.search.Search;
import org.apache.skywalking.library.elasticsearch.requests.search.SearchBuilder;
import org.apache.skywalking.library.elasticsearch.requests.search.Sort;
import org.apache.skywalking.library.elasticsearch.response.search.SearchHit;
import org.apache.skywalking.library.elasticsearch.response.search.SearchResponse;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage.AsyncProfilerTaskRecord;
import org.apache.skywalking.oap.server.core.profiling.trace.ProfileTaskRecord;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerDataFormatType;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerEventType;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.StringUtil;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.IndexController;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AsyncProfilerTaskQueryEsDAO extends EsDAO implements IAsyncProfilerTaskQueryDAO {

    private final Gson GSON = new Gson();
    private final int queryMaxSize;

    public AsyncProfilerTaskQueryEsDAO(ElasticSearchClient client, int queryMaxSize) {
        super(client);
        this.queryMaxSize = queryMaxSize;
    }

    @Override
    public List<AsyncProfilerTask> getTaskList(String serviceInstanceId, Long startTimeBucket, Long endTimeBucket, Integer limit) {
        String index = IndexController.LogicIndicesRegister.getPhysicalTableName(AsyncProfilerTaskRecord.INDEX_NAME);
        BoolQueryBuilder query = Query.bool();
        if (IndexController.LogicIndicesRegister.isMergedTable(AsyncProfilerTaskRecord.INDEX_NAME)) {
            query.must(Query.term(IndexController.LogicIndicesRegister.RECORD_TABLE_NAME, AsyncProfilerTaskRecord.INDEX_NAME));
        }

        if (StringUtil.isNotEmpty(serviceInstanceId)) {
            query.must(Query.term(AsyncProfilerTaskRecord.SERVICE_INSTANCE_ID, serviceInstanceId));
        }

        if (startTimeBucket != null) {
            query.must(Query.range(AsyncProfilerTaskRecord.TIME_BUCKET).gte(startTimeBucket));
        }

        if (endTimeBucket != null) {
            query.must(Query.range(AsyncProfilerTaskRecord.TIME_BUCKET).lte(endTimeBucket));
        }
        SearchBuilder search = Search.builder().query(query);
        search.size(Objects.requireNonNullElse(limit, queryMaxSize));

        search.sort(AsyncProfilerTaskRecord.CREATE_TIME, Sort.Order.DESC);

        final SearchResponse response = getClient().search(index, search.build());

        final List<AsyncProfilerTask> tasks = new LinkedList<>();
        for (SearchHit searchHit : response.getHits()) {
            tasks.add(parseTask(searchHit));
        }

        return tasks;
    }

    @Override
    public AsyncProfilerTask getById(String id) throws IOException {
        if (StringUtil.isEmpty(id)) {
            return null;
        }
        final String index =
                IndexController.LogicIndicesRegister.getPhysicalTableName(ProfileTaskRecord.INDEX_NAME);

        final SearchBuilder search = Search.builder()
                .query(Query.bool().must(Query.term(ProfileTaskRecord.TASK_ID, id)))
                .size(1);

        final SearchResponse response = getClient().search(index, search.build());

        if (!response.getHits().getHits().isEmpty()) {
            return parseTask(response.getHits().getHits().iterator().next());
        }
        return null;
    }

    private AsyncProfilerTask parseTask(SearchHit data) {
        Map<String, Object> source = data.getSource();
        List<String> events = GSON.fromJson((String) source.get(AsyncProfilerTaskRecord.EVENT_TYPES),
                new TypeToken<List<String>>() {
                }.getType());
        String dataFormat = (String) source.get(AsyncProfilerTaskRecord.DATA_FORMAT);
        return AsyncProfilerTask.builder()
                .id((String) source.get(AsyncProfilerTaskRecord.TASK_ID))
                .serviceId((String) source.get(AsyncProfilerTaskRecord.SERVICE_ID))
                .serviceInstanceId((String) source.get(AsyncProfilerTaskRecord.SERVICE_INSTANCE_ID))
                .createTime(((Number) source.get(AsyncProfilerTaskRecord.CREATE_TIME)).longValue())
                .duration(((Number) source.get(AsyncProfilerTaskRecord.DURATION)).intValue())
                .execArgs((String) source.get(AsyncProfilerTaskRecord.EXEC_ARGS))
                .events(AsyncProfilerEventType.valueOfList(events))
                .dataFormat(AsyncProfilerDataFormatType.valueOf(dataFormat))
                .build();
    }
}
