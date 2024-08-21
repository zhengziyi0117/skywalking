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
