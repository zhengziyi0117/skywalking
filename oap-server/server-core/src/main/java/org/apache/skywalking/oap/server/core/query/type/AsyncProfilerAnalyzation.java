package org.apache.skywalking.oap.server.core.query.type;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AsyncProfilerAnalyzation {
    private String errorReason;
    private List<AsyncProfilerStackTree> trees;
}
