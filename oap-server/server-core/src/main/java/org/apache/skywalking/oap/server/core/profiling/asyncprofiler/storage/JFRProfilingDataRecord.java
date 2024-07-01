//package org.apache.skywalking.oap.server.core.profiling.asyncprofiler.storage;
//
//import com.google.common.base.Charsets;
//import com.google.common.hash.Hashing;
//import lombok.Data;
//import org.apache.skywalking.oap.server.core.analysis.Stream;
//import org.apache.skywalking.oap.server.core.analysis.record.Record;
//import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
//import org.apache.skywalking.oap.server.core.profiling.ebpf.storage.EBPFProfilingDataRecord;
//import org.apache.skywalking.oap.server.core.storage.StorageID;
//import org.apache.skywalking.oap.server.core.storage.annotation.BanyanDB;
//import org.apache.skywalking.oap.server.core.storage.annotation.Column;
//import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
//import org.apache.skywalking.oap.server.core.storage.type.Convert2Storage;
//import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;
//
//import java.util.List;
//
//import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.JFR_PROFILING_DATA;
//
//@Data
////@Stream(name = JFRProfilingDataRecord.INDEX_NAME, scopeId = JFR_PROFILING_DATA,
////        builder = JFRProfilingDataRecord.Builder.class, processor = RecordStreamProcessor.class)
////@BanyanDB.TimestampColumn(JFRProfilingDataRecord.START_TIME)
//public class JFRProfilingDataRecord extends Record {
//    public static final String INDEX_NAME = "jfr_profiling_data";
//
//    public static final String START_TIME = "start_time";
//    public static final String TASK_ID = "task_id";
//    public static final String STACK_FRAMES = "stack_frames";
//    public static final String DURATION = "duration";
//    public static final String INTERVAL = "interval";
//    public static final String TLAB_SIZE = "tlab_size";
//
//    @Column(name = TASK_ID)
//    private long taskId;
//    @Column(name = START_TIME)
//    private long startTime;
//    @Column(name = STACK_FRAMES, storageOnly = true)
//    private List<String> stackFrames;
//    // unit ns
//    private long duration;
//    // unit byte
//    private long tlabSize;
//
//    @Override
//    public StorageID id() {
//        return new StorageID().appendMutant(
//                new String[]{
//                        TASK_ID,
//                        START_TIME
//                },
//                Hashing.sha256().newHasher()
//                        .putLong(taskId)
//                        .putLong(startTime)
//                        .hash().toString()
//        );
//    }
//
//    public static class Builder implements StorageBuilder<JFRProfilingDataRecord> {
//
//        @Override
//        public JFRProfilingDataRecord storage2Entity(final Convert2Entity converter) {
//            final JFRProfilingDataRecord dataTraffic = new JFRProfilingDataRecord();
//            dataTraffic.setTimeBucket(((Number) converter.get(TIME_BUCKET)).longValue());
//            dataTraffic.setTaskId(((Number) converter.get(TASK_ID)).longValue());
//            dataTraffic.setStartTime(((Number) converter.get(START_TIME)).longValue());
//            dataTraffic.setStackFrames((List<String>) converter.get(STACK_FRAMES));
//            dataTraffic.setEventType(((Number) converter.get(EVENT_TYPE)).intValue());
//            dataTraffic.setDuration(((Number) converter.get(DURATION)).longValue());
//            dataTraffic.setTlabSize(((Number) converter.get(TLAB_SIZE)).longValue());
//            return dataTraffic;
//        }
//
//        @Override
//        public void entity2Storage(final JFRProfilingDataRecord storageData, final Convert2Storage converter) {
//            converter.accept(TIME_BUCKET, storageData.getTimeBucket());
//            converter.accept(TASK_ID, storageData.getTaskId());
//            converter.accept(START_TIME, storageData.getStartTime());
//            converter.accept(STACK_FRAMES, storageData.getStackFrames());
//            converter.accept(EVENT_TYPE, storageData.getEventType());
//            converter.accept(DURATION, storageData.getDuration());
//            converter.accept(TLAB_SIZE, storageData.getTlabSize());
//        }
//    }
//
//}
