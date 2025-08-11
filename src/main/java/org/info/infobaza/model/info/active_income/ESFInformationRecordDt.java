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
    private String iin_bin_pokup;
    private String iin_bin_prod;
    private LocalDate date;
    private String database;
    private String aktivy;
    private String oper;
    private String dopinfo;
    private String num_doc;
    private String summ;

}
