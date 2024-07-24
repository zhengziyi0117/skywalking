package one.convert;

import one.jfr.JfrReader;
import one.jfr.StackTrace;
import one.jfr.event.AllocationSample;
import one.jfr.event.Event;
import one.jfr.event.EventAggregator;
import one.jfr.event.JfrEventType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static one.convert.Frame.TYPE_INLINED;
import static one.convert.Frame.TYPE_KERNEL;
import static one.convert.Frame.TYPE_NATIVE;

public class JfrToFrameTree extends JfrConverter {

    private final Map<JfrEventType, FrameTree> event2builderMap = new HashMap<>();

    public JfrToFrameTree(JfrReader jfr, Arguments args) {
        super(jfr, args);
    }

    @Override
    protected void convertChunk() throws IOException {
        Map<JfrEventType, EventAggregator> event2aggMap = collectMultiEvents();
        for (Map.Entry<JfrEventType, EventAggregator> entry : event2aggMap.entrySet()) {
            JfrEventType event = entry.getKey();
            EventAggregator agg = entry.getValue();
            FrameTreeBuilder frameTreeBuilder = new FrameTreeBuilder(args);

            agg.forEach(new EventAggregator.Visitor() {
                final CallStack stack = new CallStack();
                final double ticksToNanos = 1e9 / jfr.ticksPerSec;
                final boolean scale = args.total && args.lock && ticksToNanos != 1.0;

                @Override
                public void visit(Event event, long value) {
                    StackTrace stackTrace = jfr.stackTraces.get(event.stackTraceId);
                    if (stackTrace != null) {
                        Arguments args = JfrToFrameTree.this.args;
                        long[] methods = stackTrace.methods;
                        byte[] types = stackTrace.types;
                        int[] locations = stackTrace.locations;

                        if (args.threads) {
                            stack.push(getThreadName(event.tid), TYPE_NATIVE);
                        }
                        if (args.classify) {
                            Category category = getCategory(stackTrace);
                            stack.push(category.title, category.type);
                        }
                        for (int i = methods.length; --i >= 0; ) {
                            String methodName = getMethodName(methods[i], types[i]);
                            int location;
                            if (args.lines && (location = locations[i] >>> 16) != 0) {
                                methodName += ":" + location;
                            } else if (args.bci && (location = locations[i] & 0xffff) != 0) {
                                methodName += "@" + location;
                            }
                            stack.push(methodName, types[i]);
                        }
                        long classId = event.classId();
                        if (classId != 0) {
                            stack.push(getClassName(classId), (event instanceof AllocationSample)
                                    && ((AllocationSample) event).tlabSize == 0 ? TYPE_KERNEL : TYPE_INLINED);
                        }

                        frameTreeBuilder.addSample(stack, scale ? (long) (value * ticksToNanos) : value);
                        stack.clear();
                    }
                }
            });
            event2builderMap.put(event, frameTreeBuilder.build());
        }
    }

    public Map<JfrEventType, FrameTree> getFrameTreeMap() {
        return event2builderMap;
    }

}
