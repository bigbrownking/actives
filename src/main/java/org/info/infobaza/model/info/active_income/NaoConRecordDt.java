package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class NaoConRecordDt implements RecordDt{
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
    private String vid_ned;
    private String podrod_vid_ned;
    private String kd_fixed;
    private String rka;
    private String address;
    private String obshaya_ploshad;
    private String summ;

    public NaoConRecordDt(String iin_bin, String iin_bin_pokup, String iin_bin_prod, LocalDate date, String database, String aktivy, String oper, String dopinfo, String num_doc, String vid_ned, String podrod_vid_ned, String kd_fixed, String rka, String address, String obshaya_ploshad, String summ) {
        this.iin_bin = iin_bin;
        this.iin_bin_pokup = iin_bin_pokup;
        this.iin_bin_prod = iin_bin_prod;
        this.date = date;
        this.database = database;
        this.aktivy = aktivy;
        this.oper = oper;
        this.dopinfo = dopinfo;
        this.num_doc = num_doc;
        this.vid_ned = vid_ned;
        this.podrod_vid_ned = podrod_vid_ned;
        this.kd_fixed = kd_fixed;
        this.rka = rka;
        this.address = address;
        this.obshaya_ploshad = obshaya_ploshad;
        this.summ = summ;
    }
}
