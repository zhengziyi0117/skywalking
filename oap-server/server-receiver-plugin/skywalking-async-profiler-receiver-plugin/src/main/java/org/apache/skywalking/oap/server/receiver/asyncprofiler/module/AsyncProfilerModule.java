package org.apache.skywalking.oap.server.receiver.asyncprofiler.module;

import org.apache.skywalking.oap.server.library.module.ModuleDefine;

public class AsyncProfilerModule extends ModuleDefine {
    public static final String NAME = "receiver-async-profiler";

    public AsyncProfilerModule() {
        super(NAME);
    }

    @Override
    public Class[] services() {
        return new Class[] {};
    }
}
