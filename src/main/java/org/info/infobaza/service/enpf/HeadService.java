package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.job.Head;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.active_income.EsfOverall;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.StatusRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.util.convert.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HeadService {
    private final JdbcTemplate jdbcTemplate;
    private final Mapper mapper;
    private final Analyzer analyzer;
    private final PortretService portretService;

    public Head constructHead(String iin, String dateFrom, String dateTo) {
        List<SupervisorRecord> supervisorRecords = getType(iin);
        List<CompanyRecord> companyRecords = new ArrayList<>();
        for (SupervisorRecord supervisorRecord : supervisorRecords) {
            List<CompanyRecord> companyRecordsSmall = getCompanyInfo(supervisorRecord.getTaxpayer_iin_bin());
            if (companyRecordsSmall != null) {
                companyRecords.addAll(companyRecordsSmall);
            }
        }

        return Head.builder()
                .head(supervisorRecords)
                .statuses(getStatuses(iin))
                .oked(companyRecords)
                .esf(getESFInfo(iin, dateFrom, dateTo))
                .income(getIncome(iin, dateFrom, dateTo))
                .tax(0.0)
                .build();
    }

    public List<SupervisorRecord> getType(String iin) {
        String sqlSup = "SELECT * FROM pfr_dashboard.rucovoditeli WHERE employee_iin_bin = ?";
        List<SupervisorRecord> supervisorRecordSup = new ArrayList<>();
        List<SupervisorRecord> supervisorRecordFou = new ArrayList<>();
        try {
            supervisorRecordSup = jdbcTemplate.query(
                    sqlSup,
                    new Object[]{iin},
                    mapper::mapRowToSupervisor);
            log.info("supervisor: {}", supervisorRecordSup);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No руководитель found for IIN: {}", iin);
        }
        String sqlFou = "SELECT * FROM pfr_dashboard.uchrediteli WHERE founder_iin_bin = ?";
        try {
            supervisorRecordFou = jdbcTemplate.query(
                    sqlFou,
                    new Object[]{iin},
                    mapper::mapRowToFounder);
            log.info("founder: {}", supervisorRecordFou);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No учредитель found for IIN: {}", iin);
        }
        if (supervisorRecordSup.isEmpty() && supervisorRecordFou.isEmpty()) {
            return new ArrayList<>();
        }
        if (!supervisorRecordSup.isEmpty() && !supervisorRecordFou.isEmpty()) {
            log.warn("found founder for IIN: {}", iin);
            return keepDistinctSupervisorRecords(supervisorRecordFou);
        } else if (supervisorRecordFou.isEmpty() && !supervisorRecordSup.isEmpty()) {
            log.warn("found supervisor for IIN: {}", iin);
            return keepDistinctSupervisorRecords(supervisorRecordSup);
        } else if (!supervisorRecordFou.isEmpty() && supervisorRecordSup.isEmpty()) {
            log.warn("found founder for IIN: {}", iin);
            return keepDistinctSupervisorRecords(supervisorRecordFou);
        }
        return new ArrayList<>();
    }

    private List<CompanyRecord> getCompanyInfo(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch company info");
            return null;
        }
        String sql = "SELECT * FROM gdb_ul0205.ztFaces WHERE `BIN subekta` = ?";
        try {
            List<CompanyRecord> companyRecords = jdbcTemplate.query(sql, new Object[]{iin}, mapper::mapRowToCompany);
            if (companyRecords == null || companyRecords.isEmpty()) {
                log.warn("No company found for IIN: {}", iin);
                return null;
            }

            Map<String, CompanyRecord> latestRecords = companyRecords.stream()
                    .collect(Collectors.toMap(
                            CompanyRecord::getOrigName,
                            record -> record,
                            (existing, replacement) -> {
                                if (existing.getDateReg() == null) {
                                    return replacement;
                                }
                                if (replacement.getDateReg() == null) {
                                    return existing;
                                }
                                return existing.getDateReg().isAfter(replacement.getDateReg()) ? existing : replacement;
                            }
                    ));

            return new ArrayList<>(latestRecords.values());
        } catch (Exception e) {
            log.error("Error fetching company info for IIN: {}", iin, e);
            return null;
        }
    }

    private Double getIncome(String iin, String dateFrom, String dateTo) {
        return analyzer.calculateTotalIncomeByIIN(iin, dateFrom, dateTo);
    }

    private List<EsfOverall> getESFInfo(String iin, String dateFrom, String dateTo) {
        String sql = "select * from pfr_dashboard.esf_work_overall where iin_bin = ? AND date BETWEEN ? AND ?";
        return jdbcTemplate.query(
                sql, new Object[]{iin, dateFrom, dateTo}, mapper::mapRowToESFOverall
        );
    }

    private List<String> getStatuses(String iin) {
        String sql = "select * from pfr_dashboard.priznak_neblagodeznyh where iin_bin = ?";

        List<StatusRecord> statusRecords = jdbcTemplate.query(
                sql, new Object[]{iin}, mapper::mapRowToStatus
        );

        return statusRecords.stream().map(StatusRecord::getStatus).toList();
    }


    private List<SupervisorRecord> keepDistinctSupervisorRecords(List<SupervisorRecord> supervisorRecords) {
        return supervisorRecords.stream().distinct().toList();
    }


}
