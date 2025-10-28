package org.info.infobaza.model.info.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "controller_name", length = 100)
    private String controllerName;

    @Column(name = "method_name", length = 100)
    private String methodName;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "search_by", length = 50)
    private String searchBy;

    @Lob
    @Column(name = "request_args")
    private String requestArgs;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "success")
    private Boolean success;
}
