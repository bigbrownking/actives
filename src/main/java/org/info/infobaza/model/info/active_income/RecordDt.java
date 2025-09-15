package org.info.infobaza.model.info.active_income;

import java.time.LocalDate;

public interface RecordDt {
    String getIin_bin();
    LocalDate getDate();
    String getDatabase();
    String getOper();
    String getDopinfo();
    String getSumm();
    String getAktivy();

    void setIin_bin(String iin);
    void setDate(LocalDate localDate);
    void setDatabase(String database);
    void setOper(String oper);
    void setDopinfo(String dopinfo);
    void setSumm(String summ);
    void setAktivy(String aktivy);

}