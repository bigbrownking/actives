package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class UlRecordDt implements RecordDt {
    private String iin_bin;
    private String type;
    private String name;
    private LocalDate date;
    private String database;
    private String aktivy;
    private String oper;
    private String dopinfo;
    private String summ;
}
