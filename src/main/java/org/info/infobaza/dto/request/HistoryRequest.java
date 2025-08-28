package org.info.infobaza.dto.request;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class HistoryRequest {
    private String iinBin;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
