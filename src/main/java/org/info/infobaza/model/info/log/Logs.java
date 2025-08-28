package org.info.infobaza.model.info.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Logs {
    private String fileName;
    private LocalDateTime timestamp;
    private String clientIp;
    private Map<String, Object> requestArgs;
    private Long executionTimeMs;
    private long lineNumber;
}