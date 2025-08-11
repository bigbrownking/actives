package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class ActiveOverall {
    private String iin_bin;
    private LocalDate date;
    private String database;
    private String aktyvy;
    private String oper;
    private String dopinfo;
    private Double summ;

}
