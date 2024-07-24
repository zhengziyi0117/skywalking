package one.convert;

import one.jfr.JfrReader;
import one.jfr.event.JfrEventType;

import java.io.IOException;
import java.util.Map;

public class JfrParser {

    public static Map<JfrEventType, FrameTree> dumpTree(String fileName, Arguments args) throws IOException {
        try (JfrReader jfr = new JfrReader(fileName)) {
            JfrToFrameTree converter = new JfrToFrameTree(jfr, args);
            converter.convert();
            return converter.getFrameTreeMap();
//            JfrToFlame converter = new JfrToFlame(jfr, args);
//            converter.convert();
//            converter.dump(null);
//            return null;
        }
    }

}
