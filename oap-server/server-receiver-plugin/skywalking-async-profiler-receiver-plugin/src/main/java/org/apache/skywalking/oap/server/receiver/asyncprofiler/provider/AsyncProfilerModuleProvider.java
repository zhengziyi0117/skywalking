package org.apache.skywalking.oap.server.receiver.asyncprofiler.provider;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.receiver.asyncprofiler.module.AsyncProfilerModule;
import org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler.AsyncProfilerServiceHandler;
import org.apache.skywalking.oap.server.receiver.sharing.server.SharingServerModule;

public class AsyncProfilerModuleProvider extends ModuleProvider {
    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return AsyncProfilerModule.class;
    }

    @Override
    public ConfigCreator<? extends ModuleConfig> newConfigCreator() {
        return null;
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                .provider()
                .getService(GRPCHandlerRegister.class);
        AsyncProfilerServiceHandler asyncProfilerServiceHandler = new AsyncProfilerServiceHandler(getManager());
        grpcHandlerRegister.addHandler(asyncProfilerServiceHandler);
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override
    public String[] requiredModules() {
        return new String[]{
                CoreModule.NAME,
                SharingServerModule.NAME
        };
    }
}
