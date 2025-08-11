package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class InformationRecordDt implements RecordDt {
    private String iin_bin;
    private LocalDate date;
    private String database;
    private String aktyvy;
    private String oper;
    private String dopinfo;
    private String summ;


    public InformationRecordDt(String iin_bin, LocalDate date, String database, String oper, String dopinfo, String summ) {
        this.iin_bin = iin_bin;
        this.date = date;
        this.database = database;
        this.oper = oper;
        this.dopinfo = dopinfo;
        this.summ = summ;
    }
}
