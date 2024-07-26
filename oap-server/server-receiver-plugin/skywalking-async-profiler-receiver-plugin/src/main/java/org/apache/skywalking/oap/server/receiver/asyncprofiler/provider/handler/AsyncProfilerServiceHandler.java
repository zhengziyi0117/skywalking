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

package org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfileTaskCommandQuery;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerData;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerDataFormatType;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerMetaData;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerTaskGrpc;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.command.CommandService;
import org.apache.skywalking.oap.server.core.profiling.asyncprofiler.analyze.JfrAnalyzer;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.source.JfrProfilingData;
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
    private final JfrAnalyzer jfrAnalyzer;

    public AsyncProfilerServiceHandler(ModuleManager moduleManager) {
        this.metadataQueryDAO = moduleManager.find(StorageModule.NAME).provider().getService(IMetadataQueryDAO.class);
        this.taskDAO = moduleManager.find(StorageModule.NAME).provider().getService(IAsyncProfilerTaskQueryDAO.class);
        this.sourceReceiver = moduleManager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        this.commandService = moduleManager.find(CoreModule.NAME).provider().getService(CommandService.class);
        this.jfrAnalyzer = new JfrAnalyzer(moduleManager);
    }

    @Override
    public StreamObserver<AsyncProfilerData> collect(StreamObserver<Commands> responseObserver) {
        return new StreamObserver<AsyncProfilerData>() {
            private AsyncProfilerMetaData taskMetaData;
            private Path tempFile;
            private volatile OutputStream outputStream;

            @Override
            @SneakyThrows
            public void onNext(AsyncProfilerData asyncProfilerData) {
                if (asyncProfilerData.hasMetaData()) {
                    taskMetaData = asyncProfilerData.getMetaData();
                    tempFile = Path.of("/Users/bytedance/IdeaProjects/skywalking/", taskMetaData.getTaskId());
                    outputStream = Files.newOutputStream(tempFile);
                } else if (asyncProfilerData.hasContent()) {
                    outputStream.write(asyncProfilerData.getContent().toByteArray());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("close output stream error", e);
                    throw new RuntimeException(e);
                }
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
                // save data
                try {
                    outputStream.flush();
                } finally {
                    outputStream.close();
                }
                responseObserver.onNext(Commands.newBuilder().build());
                responseObserver.onCompleted();

                parseAndStorageData(taskMetaData.getTaskId(), tempFile.getFileName().toString());
            }
        };
    }

    private void parseAndStorageData(String taskId, String fileName) throws IOException {
        // TODO storage profiling file?
        AsyncProfilerTask task = taskDAO.getById(taskId);
        if (task == null) {
            log.error("AsyncProfiler taskId:{} not found but receive data", taskId);
            return;
        }

        if (AsyncProfilerDataFormatType.JFR.equals(task.getDataFormat())) {
            List<JfrProfilingData> jfrProfilingData = jfrAnalyzer.parseJfr(taskId, fileName);
            for (JfrProfilingData data : jfrProfilingData) {
                sourceReceiver.receive(data);
            }
        } else if (AsyncProfilerDataFormatType.HTML.equals(task.getDataFormat())) {
            // storage ?
        } else {
            log.error("unknown async profiler data format type:{}", task.getDataFormat());
        }
    }

    @Override
    public void getAsyncProfileTaskCommands(AsyncProfileTaskCommandQuery request, StreamObserver<Commands> responseObserver) {
        String serviceId = IDManager.ServiceID.buildId(request.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, request.getServiceInstance());
        long latestUpdateTime = TimeBucket.getMinuteTimeBucket(request.getLastCommandTime());
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
