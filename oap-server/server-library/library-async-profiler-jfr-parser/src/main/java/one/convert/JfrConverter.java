/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.convert;

import one.jfr.ClassRef;
import one.jfr.Dictionary;
import one.jfr.JfrReader;
import one.jfr.MethodRef;
import one.jfr.event.AllocationSample;
import one.jfr.event.ContendedLock;
import one.jfr.event.Event;
import one.jfr.event.EventAggregator;
import one.jfr.event.EventPair;
import one.jfr.event.ExecutionSample;
import one.jfr.event.JfrEventType;
import one.jfr.event.LiveObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static one.convert.Frame.Entry;
import static one.convert.Frame.TYPE_CPP;
import static one.convert.Frame.TYPE_KERNEL;
import static one.convert.Frame.TYPE_NATIVE;
import static one.jfr.event.JfrEventType.EXECUTION_SAMPLE;
import static one.jfr.event.JfrEventType.JAVA_MONITOR_ENTER;
import static one.jfr.event.JfrEventType.OBJECT_ALLOCATION_IN_NEW_TLAB;
import static one.jfr.event.JfrEventType.OBJECT_ALLOCATION_OUTSIDE_TLAB;
import static one.jfr.event.JfrEventType.PROFILER_LIVE_OBJECT;
import static one.jfr.event.JfrEventType.THREAD_PARK;

public abstract class JfrConverter extends Classifier {
    protected final JfrReader jfr;
    protected final Arguments args;
    protected Dictionary<String> methodNames;

    public JfrConverter(JfrReader jfr, Arguments args) {
        this.jfr = jfr;
        this.args = args;
    }

    public void convert() throws IOException {
        jfr.stopAtNewChunk = true;
        while (jfr.hasMoreChunks()) {
            // Reset method dictionary, since new chunk may have different IDs
            methodNames = new Dictionary<>();
            convertChunk();
        }
    }

    protected abstract void convertChunk() throws IOException;

    protected EventAggregator collectEvents() throws IOException {
        EventAggregator agg = new EventAggregator(args.threads, args.total);

        Class<? extends Event> eventClass =
                args.live ? LiveObject.class :
                        args.alloc ? AllocationSample.class :
                                args.lock ? ContendedLock.class : ExecutionSample.class;

        long threadStates = 0;
        if (args.state != null) {
            for (String state : args.state.toUpperCase().split(",")) {
                threadStates |= 1L << toThreadState(state);
            }
        } else if (args.cpu) {
            threadStates = 1L << toThreadState("DEFAULT");
        } else if (args.wall) {
            threadStates = ~(1L << toThreadState("DEFAULT"));
        }

        long startTicks = args.from != 0 ? toTicks(args.from) : Long.MIN_VALUE;
        long endTicks = args.to != 0 ? toTicks(args.to) : Long.MAX_VALUE;

        for (Event event; (event = jfr.readEvent(eventClass)) != null; ) {
            if (event.time >= startTicks && event.time <= endTicks) {
                if (threadStates == 0 || (threadStates & (1L << ((ExecutionSample) event).threadState)) != 0) {
                    agg.collect(event);
                }
            }
        }

        return agg;
    }

    protected Map<JfrEventType, EventAggregator> collectMultiEvents() throws IOException {
        Map<JfrEventType, EventAggregator> event2aggMap = new HashMap<>();

        long threadStates = 0;
        if (args.state != null) {
            for (String state : args.state.toUpperCase().split(",")) {
                threadStates |= 1L << toThreadState(state);
            }
        }

        long startTicks = args.from != 0 ? toTicks(args.from) : Long.MIN_VALUE;
        long endTicks = args.to != 0 ? toTicks(args.to) : Long.MAX_VALUE;
        for (EventPair eventPair; (eventPair = jfr.readEventWithType()) != null; ) {
            JfrEventType type = eventPair.getType();
            Event event = eventPair.getEvent();
            if (event.time >= startTicks && event.time <= endTicks) {
                EventAggregator agg;
                switch (type) {
                    case EXECUTION_SAMPLE:
                        agg = event2aggMap.computeIfAbsent(EXECUTION_SAMPLE, JfrConverter::getExecutionSampleAggregator);
                        break;
                    case OBJECT_ALLOCATION_IN_NEW_TLAB:
                        agg = event2aggMap.computeIfAbsent(OBJECT_ALLOCATION_IN_NEW_TLAB, JfrConverter::getExecutionSampleAggregator);
                        break;
                    case OBJECT_ALLOCATION_OUTSIDE_TLAB:
                        agg = event2aggMap.computeIfAbsent(OBJECT_ALLOCATION_OUTSIDE_TLAB, JfrConverter::getExecutionSampleAggregator);
                        break;
                    case THREAD_PARK:
                        agg = event2aggMap.computeIfAbsent(THREAD_PARK, JfrConverter::getExecutionSampleAggregator);
                        break;
                    case JAVA_MONITOR_ENTER:
                        agg = event2aggMap.computeIfAbsent(JAVA_MONITOR_ENTER, JfrConverter::getExecutionSampleAggregator);
                        break;
                    case PROFILER_LIVE_OBJECT:
                        agg = event2aggMap.computeIfAbsent(PROFILER_LIVE_OBJECT, JfrConverter::getExecutionSampleAggregator);
                        break;
                    default:
                        throw new RuntimeException("Unknown event type: " + type);
                }
                if (!(event instanceof ExecutionSample) || (threadStates & (1L << ((ExecutionSample) event).threadState)) != 0) {
                    agg.collect(event);
                }
            }
        }


        return event2aggMap;
    }

    private static EventAggregator getExecutionSampleAggregator(JfrEventType jfrEventType) {
        // TODO aggregator default configure
        switch (jfrEventType) {
            case EXECUTION_SAMPLE:
                return new EventAggregator(false, false);
            case OBJECT_ALLOCATION_IN_NEW_TLAB:
            case OBJECT_ALLOCATION_OUTSIDE_TLAB:
                return new EventAggregator(false, true);
            case THREAD_PARK:
                return new EventAggregator(true, true);
            case JAVA_MONITOR_ENTER:
                return new EventAggregator(true, false);
            case PROFILER_LIVE_OBJECT:
                return new EventAggregator(true, false);
            default:
                return new EventAggregator(false, false);
        }
    }

    protected int toThreadState(String name) {
        Map<Integer, String> threadStates = jfr.enums.get("jdk.types.ThreadState");
        if (threadStates != null) {
            for (Entry<Integer, String> entry : threadStates.entrySet()) {
                if (entry.getValue().startsWith(name, 6)) {
                    return entry.getKey();
                }
            }
        }
        throw new IllegalArgumentException("Unknown thread state: " + name);
    }

    // millis can be an absolute timestamp or an offset from the beginning/end of the recording
    protected long toTicks(long millis) {
        long nanos = millis * 1_000_000;
        if (millis < 0) {
            nanos += jfr.endNanos;
        } else if (millis < 1500000000000L) {
            nanos += jfr.startNanos;
        }
        return (long) ((nanos - jfr.chunkStartNanos) * (jfr.ticksPerSec / 1e9)) + jfr.chunkStartTicks;
    }

    @Override
    protected String getMethodName(long methodId, byte methodType) {
        String result = methodNames.get(methodId);
        if (result == null) {
            methodNames.put(methodId, result = resolveMethodName(methodId, methodType));
        }
        return result;
    }

    private String resolveMethodName(long methodId, byte methodType) {
        MethodRef method = jfr.methods.get(methodId);
        if (method == null) {
            return "unknown";
        }

        ClassRef cls = jfr.classes.get(method.cls);
        byte[] className = jfr.symbols.get(cls.name);
        byte[] methodName = jfr.symbols.get(method.name);

        if (className == null || className.length == 0 || isNativeFrame(methodType)) {
            return new String(methodName, StandardCharsets.UTF_8);
        } else {
            String classStr = toJavaClassName(className, 0, args.dot);
            if (methodName == null || methodName.length == 0) {
                return classStr;
            }
            String methodStr = new String(methodName, StandardCharsets.UTF_8);
            return classStr + '.' + methodStr;
        }
    }

    protected String getClassName(long classId) {
        ClassRef cls = jfr.classes.get(classId);
        if (cls == null) {
            return "null";
        }
        byte[] className = jfr.symbols.get(cls.name);

        int arrayDepth = 0;
        while (className[arrayDepth] == '[') {
            arrayDepth++;
        }

        String name = toJavaClassName(className, arrayDepth, true);
        while (arrayDepth-- > 0) {
            name = name.concat("[]");
        }
        return name;
    }

    protected String getThreadName(int tid) {
        String threadName = jfr.threads.get(tid);
        return threadName == null ? "[tid=" + tid + ']' :
                threadName.startsWith("[tid=") ? threadName : '[' + threadName + " tid=" + tid + ']';
    }

    protected String toJavaClassName(byte[] symbol, int start, boolean dotted) {
        int end = symbol.length;
        if (start > 0) {
            switch (symbol[start]) {
                case 'B':
                    return "byte";
                case 'C':
                    return "char";
                case 'S':
                    return "short";
                case 'I':
                    return "int";
                case 'J':
                    return "long";
                case 'Z':
                    return "boolean";
                case 'F':
                    return "float";
                case 'D':
                    return "double";
                case 'L':
                    start++;
                    end--;
            }
        }

        if (args.norm) {
            for (int i = end - 2; i > start; i--) {
                if (symbol[i] == '/' || symbol[i] == '.') {
                    if (symbol[i + 1] >= '0' && symbol[i + 1] <= '9') {
                        end = i;
                        if (i > start + 19 && symbol[i - 19] == '+' && symbol[i - 18] == '0') {
                            // Original JFR transforms lambda names to something like
                            // pkg.ClassName$$Lambda+0x00007f8177090218/543846639
                            end = i - 19;
                        }
                    }
                    break;
                }
            }
        }

        if (args.simple) {
            for (int i = end - 2; i >= start; i--) {
                if (symbol[i] == '/' && (symbol[i + 1] < '0' || symbol[i + 1] > '9')) {
                    start = i + 1;
                    break;
                }
            }
        }

        String s = new String(symbol, start, end - start, StandardCharsets.UTF_8);
        return dotted ? s.replace('/', '.') : s;
    }

    protected boolean isNativeFrame(byte methodType) {
        // In JDK Flight Recorder, TYPE_NATIVE denotes Java native methods,
        // while in async-profiler, TYPE_NATIVE is for C methods
        return methodType == TYPE_NATIVE && jfr.getEnumValue("jdk.types.FrameType", TYPE_KERNEL) != null ||
                methodType == TYPE_CPP ||
                methodType == TYPE_KERNEL;
    }
}
