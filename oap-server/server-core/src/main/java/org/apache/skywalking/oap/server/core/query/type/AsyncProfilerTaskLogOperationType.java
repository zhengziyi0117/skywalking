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

package org.apache.skywalking.oap.server.core.query.type;

import java.util.HashMap;
import java.util.Map;

public enum AsyncProfilerTaskLogOperationType {
    // when sniffer has notified
    NOTIFIED(1), // when sniffer has execution finished to report
    EXECUTION_FINISHED(2);

    private int code;
    private static final Map<Integer, ProfileTaskLogOperationType> CACHE = new HashMap<Integer, ProfileTaskLogOperationType>();

    static {
        for (ProfileTaskLogOperationType val : ProfileTaskLogOperationType.values()) {
            CACHE.put(val.getCode(), val);
        }
    }

    /**
     * Parse opetation type by code
     */
    public static ProfileTaskLogOperationType parse(int code) {
        return CACHE.get(code);
    }

    AsyncProfilerTaskLogOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
