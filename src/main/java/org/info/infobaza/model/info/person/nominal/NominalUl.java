package org.info.infobaza.model.info.person.nominal;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class NominalUl implements Nominal{
    private String iin_bin;
    private String iin_bin_pokup;
    private String iin_bin_prod;
    private LocalDate date;
    private String database;
    private String aktivy;
    private String oper;
    private String dopinfo;
    private String num_doc;
    private int summ;
    private String iin_seller;
    private int total_turnover;

}
