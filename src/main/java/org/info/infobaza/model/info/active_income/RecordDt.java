package org.info.infobaza.model.info.active_income;

import java.time.LocalDate;

public interface RecordDt {
    String getIin_bin();
    LocalDate getDate();
    String getDatabase();
    String getOper();
    String getDopinfo();
    String getSumm();
}