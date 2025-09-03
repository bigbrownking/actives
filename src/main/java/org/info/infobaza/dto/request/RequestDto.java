package org.info.infobaza.dto.request;


import org.info.infobaza.model.main.RequestStatus;

import java.time.LocalDate;
import java.util.List;

public interface RequestDto {
    String getIin();
    LocalDate getDateFrom();
    LocalDate getDateTo();
    List<String> getYears();
    List<String> getVids();
    List<String> getTypes();
    List<String> getSources();
    List<String> getIins();
    RequestStatus getStatus();
}