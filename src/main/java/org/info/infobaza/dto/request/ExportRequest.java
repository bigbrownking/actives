package org.info.infobaza.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.info.infobaza.util.logging.Loggable;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ExportRequest {
    @NotBlank(message = "IIN is required and cannot be blank")
    @Loggable
    private String iin;

    @NotBlank(message = "dateFrom is required and cannot be blank")
    @Loggable
    private LocalDate dateFrom;

    @NotBlank(message = "dateTo is required and cannot be blank")
    @Loggable
    private LocalDate dateTo;

    @Loggable
    private List<String> yearsActive;

    @Loggable
    private List<String> yearsIncome;

    @Nullable
    @Loggable
    private List<String> vids;

    @Nullable
    @Loggable
    private List<String> types;

    @Nullable
    @Loggable
    private List<String> sources;

    @Nullable
    @Loggable
    private List<String> iins;
}
