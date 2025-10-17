package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class ESFInformationRecordDt implements RecordDt {
    private String iin_bin;
    private String name;
    private String iin_bin_pokup;
    private String iin_bin_prod;
    private LocalDate date;
    private String database;
    private String aktivy;
    private String oper;
    private String dopinfo;
    private String num_doc;
    private String nomer;
    private String summ;

    public ESFInformationRecordDt(String iin_bin, String iin_bin_pokup, String iin_bin_prod, LocalDate date, String database, String aktivy, String oper, String dopinfo, String num_doc, String summ) {
        this.iin_bin = iin_bin;
        this.iin_bin_pokup = iin_bin_pokup;
        this.iin_bin_prod = iin_bin_prod;
        this.date = date;
        this.database = database;
        this.aktivy = aktivy;
        this.oper = oper;
        this.dopinfo = dopinfo;
        this.num_doc = num_doc;
        this.summ = summ;
    }

    public ESFInformationRecordDt(String iin_bin, String iin_bin_pokup, String iin_bin_prod, LocalDate date, String database, String aktivy, String oper, String dopinfo, String num_doc, String nomer, String summ) {
        this.iin_bin = iin_bin;
        this.iin_bin_pokup = iin_bin_pokup;
        this.iin_bin_prod = iin_bin_prod;
        this.date = date;
        this.database = database;
        this.aktivy = aktivy;
        this.oper = oper;
        this.dopinfo = dopinfo;
        this.num_doc = num_doc;
        this.nomer = nomer;
        this.summ = summ;
    }
}
