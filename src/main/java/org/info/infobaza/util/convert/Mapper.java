package org.info.infobaza.util.convert;

import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.ActiveOverall;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.EsfOverall;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.StatusRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.person.NominalFiz;
import org.info.infobaza.model.info.person.PersonRecord;
import org.info.infobaza.model.info.person.RelationRecord;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


@Component
@Slf4j
public class Mapper {
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy", java.util.Locale.ROOT);

    public RelationRecord mapRowToRelation(ResultSet rs, int rowNum) throws SQLException {
        try {
            return new RelationRecord(
                    rs.getString("iin_1"),
                    rs.getString("iin_2"),
                    rs.getString("vid_sviazi"),
                    rs.getString("status"),
                    rs.getInt("level_rod")
            );
        }catch (Exception e){
            return new RelationRecord(
                    rs.getString("iin_1"),
                    rs.getString("iin_2"),
                    rs.getString("vid_sviazi"),
                    rs.getString("status"));
        }
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
                .taxpayerType(rs.getString("taxpayer_type"))
                .build();
    }
    public SupervisorRecord mapRowToFounder(ResultSet rs, int rowNum) throws SQLException {
        return SupervisorRecord.builder()
                .iin_bin(rs.getString("founder_iin_bin"))
                .positionType("Учредитель")
                .taxpayer_iin_bin(rs.getString("taxpayer_iin_bin"))
                .taxpayerType(rs.getString("taxpayer_type"))
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

}
