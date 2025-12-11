package org.info.infobaza.util;

import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;

import java.util.Objects;

public class RecordKeyGenerator {

    public static String generateKey(ESFInformationRecordDt record) {
        return String.join("|",
                Objects.toString(record.getIin_bin(), ""),
                Objects.toString(record.getIin_bin_pokup(), ""),
                Objects.toString(record.getIin_bin_prod(), ""),
                Objects.toString(record.getDate(), ""),
                Objects.toString(record.getDatabase(), ""),
                Objects.toString(record.getAktivy(), ""),
                Objects.toString(record.getOper(), ""),
                Objects.toString(record.getSumm(), "")
        );
    }

    public static String generateKey(InformationRecordDt record) {
        return String.join("|",
                Objects.toString(record.getIin_bin(), ""),
                Objects.toString(record.getDate(), ""),
                Objects.toString(record.getDatabase(), ""),
                Objects.toString(record.getAktivy(), ""),
                Objects.toString(record.getOper(), ""),
                Objects.toString(record.getDopinfo(), ""),
                Objects.toString(record.getSumm(), "")
        );
    }

    public static String generateKey(NaoConRecordDt record) {
        return String.join("|",
                Objects.toString(record.getIin_bin(), ""),
                Objects.toString(record.getIin_bin_pokup(), ""),
                Objects.toString(record.getIin_bin_prod(), ""),
                Objects.toString(record.getDate(), ""),
                Objects.toString(record.getDatabase(), ""),
                Objects.toString(record.getAktivy(), ""),
                Objects.toString(record.getOper(), ""),
                Objects.toString(record.getNum_doc(), ""),
                Objects.toString(record.getKd_fixed(), ""),
                Objects.toString(record.getSumm(), "")
        );
    }
}