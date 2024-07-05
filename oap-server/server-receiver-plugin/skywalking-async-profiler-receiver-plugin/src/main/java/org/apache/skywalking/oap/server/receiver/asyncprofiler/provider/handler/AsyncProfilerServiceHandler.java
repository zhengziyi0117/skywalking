package org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfileTaskCommandQuery;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerData;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerMetaData;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerTaskGrpc;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.command.CommandService;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.analyze.JFRAnalyzer;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.core.storage.query.IMetadataQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.network.trace.component.command.AsyncProfilerTaskCommand;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class AsyncProfilerServiceHandler extends AsyncProfilerTaskGrpc.AsyncProfilerTaskImplBase implements GRPCHandler {

    private final IMetadataQueryDAO metadataQueryDAO;
    private final IAsyncProfilerTaskQueryDAO taskDAO;
    private final SourceReceiver sourceReceiver;
    private final CommandService commandService;
    private final JFRAnalyzer jfrAnalyzer;

    public AsyncProfilerServiceHandler(ModuleManager moduleManager) {
        this.metadataQueryDAO = moduleManager.find(StorageModule.NAME).provider().getService(IMetadataQueryDAO.class);
        this.taskDAO = moduleManager.find(StorageModule.NAME).provider().getService(IAsyncProfilerTaskQueryDAO.class);
        this.sourceReceiver = moduleManager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        this.commandService = moduleManager.find(CoreModule.NAME).provider().getService(CommandService.class);
        this.jfrAnalyzer = new JFRAnalyzer();
    }

    @Override
    public StreamObserver<AsyncProfilerData> collect(StreamObserver<Commands> responseObserver) {
        return new StreamObserver<AsyncProfilerData>() {
            private AsyncProfilerMetaData taskMetaData;

            private volatile OutputStream outputStream;

            @Override
            @SneakyThrows
            public void onNext(AsyncProfilerData asyncProfilerData) {
                if (asyncProfilerData.hasMetaData()) {
                    taskMetaData = asyncProfilerData.getMetaData();
//                    Path tempFile = Files.createTempFile(taskMetaData.getTaskId(), "");
                    Path tempFile = Files.createFile(Path.of("/Users/bytedance/IdeaProjects/skywalking/", taskMetaData.getTaskId()));
                    outputStream = Files.newOutputStream(tempFile);
                } else if (asyncProfilerData.hasContent()) {
                    outputStream.write(asyncProfilerData.getContent().toByteArray());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Status status = Status.fromThrowable(throwable);
                if (Status.CANCELLED.getCode() == status.getCode()) {
                    if (log.isDebugEnabled()) {
                        log.debug(throwable.getMessage(), throwable);
                    }
                    return;
                }
                log.error("Error in receiving async profiler profiling data", throwable);
            }

            @Override
            @SneakyThrows
            public void onCompleted() {
                try {
                    outputStream.flush();
                } finally {
                    outputStream.close();
                }
                responseObserver.onNext(Commands.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void getAsyncProfileTaskCommands(AsyncProfileTaskCommandQuery request, StreamObserver<Commands> responseObserver) {
        String serviceId = IDManager.ServiceID.buildId(request.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, request.getServiceInstance());
        long latestUpdateTime = request.getLastCommandTime();
        try {
            // fetch tasks from process id list
            List<AsyncProfilerTask> taskList = taskDAO.getTaskList(serviceInstanceId, latestUpdateTime, null, 1);
            if (CollectionUtils.isEmpty(taskList)) {
                responseObserver.onNext(Commands.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }
            AsyncProfilerTask task = taskList.get(0);
            AsyncProfilerTaskCommand asyncProfilerTaskCommand = commandService.newAsyncProfileTaskCommand(task);
            Commands commands = Commands.newBuilder().addCommands(asyncProfilerTaskCommand.serialize()).build();
            responseObserver.onNext(commands);
            responseObserver.onCompleted();
            return;
        } catch (IOException e) {
            log.warn("query async profiler process profiling task failure", e);
            responseObserver.onNext(Commands.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
    }
}
