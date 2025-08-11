package org.info.infobaza.model.info;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CompanyRecord {
    private String rusName;
    private String origName;
    private String bin;
    private LocalDate dateReg;
    private String telephone;
}
