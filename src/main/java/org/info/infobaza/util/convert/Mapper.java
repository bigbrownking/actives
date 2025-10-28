package org.info.infobaza.util.convert;

import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.*;
import org.info.infobaza.model.info.job.*;
import org.info.infobaza.model.info.object.Car;
import org.info.infobaza.model.info.object.CarInsurance;
import org.info.infobaza.model.info.person.nominal.NominalFiz;
import org.info.infobaza.model.info.person.nominal.NominalRucUchr;
import org.info.infobaza.model.info.person.nominal.NominalUl;
import org.info.infobaza.model.info.person.PersonRecord;
import org.info.infobaza.model.info.person.RelationRecord;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


@Component
@Slf4j
public class Mapper {
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT);


    public RelationRecord mapRowToSecRelation(ResultSet rs, int rowNum) {
        try {
            String vid = rs.getString("status");
            Map<String, String> dopinfo = new HashMap<>();
            for (int i = 1; i <= 8; i++) {
                String columnName = "col" + i;
                try {
                    String value = rs.getString(columnName);
                    String key = null;
                    if (value != null && !value.isEmpty()) {
                        if ((vid.equals("Вместе жили в 2 и более адресах") || vid.equals("Вместе работали")) && columnName.equals("col1"))
                            key = "AP code";
                        if ((vid.equals("Вместе жили в 2 и более адресах") || vid.equals("Вместе работали")) && columnName.equals("col2"))
                            key = "Registration date";
                        if ((vid.equals("Вместе жили в 2 и более адресах") || vid.equals("Вместе работали")) && columnName.equals("col3"))
                            key = "End registration date";

                        if (vid.equals("Совместные автостраховки") && columnName.equals("col1")) key = "GRNZ";
                        if (vid.equals("Совместные автостраховки") && columnName.equals("col2"))
                            key = "Save begin date";
                        if (vid.equals("Совместные автостраховки") && columnName.equals("col3")) key = "Save end date";
                        if (vid.equals("Совместные автостраховки") && columnName.equals("col4")) key = "VIN code";

                        if ((vid.equals("Поступили ДС") || vid.equals("Перечислил ДС")) && columnName.equals("col1"))
                            key = "Operation date";
                        if ((vid.equals("Поступили ДС") || vid.equals("Перечислил ДС")) && columnName.equals("col2"))
                            key = "Operation summ";
                        if ((vid.equals("Поступили ДС") || vid.equals("Перечислил ДС")) && columnName.equals("col3"))
                            key = "Operation name";

                        if (vid.equals("Коммунальные платежи") && columnName.equals("col1")) key = "Summ";
                        if (vid.equals("Коммунальные платежи") && columnName.equals("col2")) key = "For";
                        if (vid.equals("Коммунальные платежи") && columnName.equals("col3")) key = "Number";

                        if (vid.equals("Налоги") && columnName.equals("col1")) key = "Tax for";
                        if (vid.equals("Налоги") && columnName.equals("col2")) key = "BVU";
                        if (vid.equals("Налоги") && columnName.equals("col3")) key = "Tax number";
                        if (vid.equals("Налоги") && columnName.equals("col4")) key = "Summ";
                        if (vid.equals("Налоги") && columnName.equals("col5")) key = "UGD";
                        if (vid.equals("Налоги") && columnName.equals("col6")) key = "KNP";
                        if (vid.equals("Налоги") && columnName.equals("col7")) key = "KBK";
                        if (vid.equals("Налоги") && columnName.equals("col8")) key = "Purpose of tax";

                        if (vid.equals("Доверенность") && columnName.equals("col2")) key = "Registration_date";
                        if (vid.equals("Доверенность") && columnName.equals("col1")) key = "For_dover";

                        dopinfo.put(key, value);
                    } else {
                    }
                } catch (SQLException e) {
                    log.warn("Column {} not found in ResultSet for row {}, setting to empty string", columnName, rowNum);
                }
            }

            return new RelationRecord(
                    rs.getString("iin_1"),
                    rs.getString("iin_2"),
                    rs.getString("vid_sviazi"),
                    vid,
                    dopinfo
            );
        } catch (SQLException e) {
            return null;
        }
    }

    public RelationRecord mapRowToPriRelation(ResultSet rs, int rowNum) throws SQLException {
        return new RelationRecord(
                rs.getString("iin_1"),
                rs.getString("iin_2"),
                rs.getString("vid_sviazi"),
                rs.getString("status"),
                rs.getInt("level_rod")
        );
    }

    public InformationRecordDt mapRowToInformation(ResultSet rs, int rowNum) throws SQLException {
        return new InformationRecordDt(
                rs.getString("iin_bin"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("summ")
        );

    }

    public Car mapRowToCar(ResultSet rs, int rowNum) throws SQLException {
        return new Car(
                rs.getString("marka"),
                null,
                rs.getString("vin_kod"),
                rs.getString("registration_number"),
                rs.getInt("year_of_release"),
                rs.getInt("engine_capacity"),
                rs.getString("iin_owners"),
                rs.getObject("date_of_registration", LocalDate.class),
                rs.getString("class")
        );
    }

    public CarInsurance mapRowToCarInsurance(ResultSet rs, int rowNum) throws SQLException {
        return new CarInsurance(
                rs.getString("marka"),
                rs.getString("model"),
                rs.getString("vin_kod"),
                rs.getString("grnz"),
                rs.getString("iin_strahovatel"),
                rs.getString("iin_zastrahovanny"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("finish_date", LocalDate.class)
        );
    }

    public ActiveOverall mapRowToActivesOverall(ResultSet rs, int rowNum) throws SQLException {
        return new ActiveOverall(
                rs.getString("iin_bin"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getDouble("summ")
        );
    }

    public InformationRecordDt mapRowToIncome(ResultSet rs, int rowNum) throws SQLException {
        return new InformationRecordDt(
                rs.getString("iin_bin"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("summ")
        );
    }

    public ESFInformationRecordDt mapRowToESF(ResultSet rs, int rowNum) throws SQLException {
        return new ESFInformationRecordDt(
                rs.getString("iin_bin"),
                rs.getString("iin_bin_pokup"),
                rs.getString("iin_bin_prod"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("num_doc"),
                rs.getString("summ")
        );
    }

    public PenaltyRecord mapRowToPenalty(ResultSet rs, int rowNum) throws SQLException {
        return new PenaltyRecord(
                rs.getString("iin_bin"),
                rs.getString("iin_bin_pokup"),
                rs.getString("iin_bin_prod"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("place"),
                rs.getString("summ")
        );
    }

    public ESFInformationRecordDt mapRowToUtil(ResultSet rs, int rowNum) throws SQLException {
        return new ESFInformationRecordDt(
                rs.getString("iin_bin"),
                rs.getString("iin_bin_pokup"),
                rs.getString("iin_bin_prod"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("za_cto"),
                rs.getString("nomer_licevoy_chet"),
                rs.getString("summ")
        );
    }

    public NaoConRecordDt mapRowToNaoCon(ResultSet rs, int rowNum) throws SQLException {
        return new NaoConRecordDt(
                rs.getString("iin_bin"),
                rs.getString("iin_bin_pokup"),
                rs.getString("iin_bin_prod"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("num_doc"),
                rs.getString("vid_ned"),
                rs.getString("podrod_vid_ned"),
                rs.getString("kd_fixed"),
                rs.getString("rka"),
                rs.getString("adress"),
                rs.getString("obshaya_ploshad"),
                rs.getString("summ")
        );
    }

    public EsfOverall mapRowToESFOverall(ResultSet rs, int rowNum) throws SQLException {
        return new EsfOverall(
                rs.getString("iin_bin"),
                rs.getObject("date", LocalDate.class),
                rs.getString("aktivy"),
                rs.getLong("summ")
        );
    }

    public PersonRecord mapRowToPortret(ResultSet rs, int rowNum) throws SQLException {
        return new PersonRecord(
                rs.getString("IIN"),
                rs.getString("portret")
        );
    }

    public SupervisorRecord mapRowToSupervisor(ResultSet rs, int rowNum) throws SQLException {
        return SupervisorRecord.builder()
                .iin_bin(rs.getString("employee_iin_bin"))
                .positionType(rs.getString("position_type"))
                .taxpayer_iin_bin(rs.getString("taxpayer_iin_bin"))
                .build();
    }

    public SupervisorRecord mapRowToFounder(ResultSet rs, int rowNum) throws SQLException {
        return SupervisorRecord.builder()
                .iin_bin(rs.getString("founder_iin_bin"))
                .positionType("Учредитель")
                .taxpayer_iin_bin(rs.getString("taxpayer_iin_bin"))
                .taxpayerName(rs.getString("taxpayer_name"))
                .build();
    }

    public CompanyRecord mapRowToCompany(ResultSet rs, int rowNum) throws SQLException {
        String dateStr = rs.getString("Data reg deystv");
        LocalDate dateReg = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                dateReg = LocalDate.parse(dateStr, DATE_FORMATTER);
                log.debug("Successfully parsed date '{}' for BIN: {} using pattern dd-MM-yyyy", dateStr, rs.getString("BIN subekta"));
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse date '{}' for BIN: {}, expected format dd-MM-yyyy", dateStr, rs.getString("BIN subekta"));
            }
        }

        return new CompanyRecord(
                rs.getString("Polnoe Naimen na rus yazyke"),
                rs.getString("Polnoe naim na gos yazyke"),
                rs.getString("BIN subekta"),
                dateReg,
                rs.getString("Telefon")
        );
    }

    public StatusRecord mapRowToStatus(ResultSet rs, int rowNum) throws SQLException {
        return new StatusRecord(
                rs.getString("iin_bin"),
                rs.getString("type_neblag")
        );
    }

    public InformationRecordDt mapRowToPension(ResultSet rs, int rowNum) throws SQLException {
        return new InformationRecordDt(
                rs.getString("IIN"),
                rs.getObject("PAY_DATE", LocalDate.class),
                rs.getString("KNP"),
                rs.getString("P_RNN"),
                rs.getString("P_NAME"),
                rs.getString("AMOUNT")
        );
    }

    public NominalFiz mapRowToNominalFiz(ResultSet rs, int rowNum) throws SQLException {
        return new NominalFiz(
                rs.getString("iin_bin"),
                rs.getInt("first_asset_year"),
                rs.getLong("total_sum"),
                rs.getDouble("avg_month_income"),
                rs.getDouble("ratio")
        );
    }

    public NominalUl mapRowToNominalUl(ResultSet rs, int rowNum) throws SQLException {
        return new NominalUl(
                rs.getString("iin_bin"),
                rs.getString("iin_bin_pokup"),
                rs.getString("iin_bin_prod"),
                rs.getObject("date", LocalDate.class),
                rs.getString("database"),
                rs.getString("aktivy"),
                rs.getString("oper"),
                rs.getString("dopinfo"),
                rs.getString("num_doc"),
                rs.getInt("summ"),
                rs.getString("IIN_SELLER"),
                rs.getInt("total_turnover")
        );
    }

    public NominalRucUchr mapRowToNominalRucUch(ResultSet rs, int rowNum) throws SQLException {
        return new NominalRucUchr(
                rs.getString("ruc_uch_iin"),
                rs.getString("yl_iin"),
                rs.getString("rol")
        );
    }

    public TurnoverRecord mapRowToTurnover(ResultSet rs, int rowNum) throws SQLException {
        return new TurnoverRecord(
                rs.getString("iin_bin"),
                rs.getString("bank_name"),
                rs.getString("bank_account"),
                rs.getString("summ"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getString("db_source")
        );
    }

    public HistorySupervisorRecord mapRowToHistorySupervisor(ResultSet rs, int rowNum) throws SQLException {
        try {
            return HistorySupervisorRecord.builder()
                    .iinBinCompany(rs.getString("iin_bin_company"))
                    .iinBin(rs.getString("iin_bin_ruk"))
                    .build();
        } catch (Exception e) {
            return HistorySupervisorRecord.builder()
                    .iinBinCompany(rs.getString("iin_bin_company"))
                    .iinBin(rs.getString("iin_bin_uchr"))
                    .build();
        }
    }

}
