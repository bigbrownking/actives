package org.info.infobaza.dto.request;

import lombok.Getter;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

@Getter
public class LogSearchRequest {
    @Nullable
    private LocalDate dateFrom;
    @Nullable
    private LocalDate dateTo;
    @Nullable
    private String iin_bin;
}
