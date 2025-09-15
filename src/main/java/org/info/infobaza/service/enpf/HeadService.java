package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.job.Head;
import org.info.infobaza.model.info.active_income.EsfOverall;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.HistorySupervisorRecord;
import org.info.infobaza.model.info.job.StatusRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
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
    private final SQLFileUtil sqlFileUtil;
    private final Analyzer analyzer;

    public Head constructHead(String iin, String dateFrom, String dateTo) throws IOException {
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

    public List<SupervisorRecord> getType(String iin) throws IOException {
        String sqlSup = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_ruk.getPath(), iin);
        List<SupervisorRecord> supervisorRecordSup = new ArrayList<>();
        List<SupervisorRecord> supervisorRecordFou = new ArrayList<>();
        List<HistorySupervisorRecord> historySupervisorRecordSup = new ArrayList<>();
        List<HistorySupervisorRecord> historySupervisorRecordFou = new ArrayList<>();
        try {
            supervisorRecordSup = jdbcTemplate.query(sqlSup, mapper::mapRowToSupervisor);
            log.info("supervisor: {}", supervisorRecordSup);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No руководитель found for IIN: {}", iin);
        }
        String sqlFou = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_uchr.getPath(), iin);
        try {
            supervisorRecordFou = jdbcTemplate.query(sqlFou, mapper::mapRowToFounder);
            log.info("founder: {}", supervisorRecordFou);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No учредитель found for IIN: {}", iin);
        }

        String sqlHistSup = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_hist_ruk.getPath(), iin);
        try {
            historySupervisorRecordSup = jdbcTemplate.query(sqlHistSup, mapper::mapRowToHistorySupervisor);
            log.info("history supervisor: {}", historySupervisorRecordSup);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No руководитель found for IIN: {}", iin);
        }


        String sqlHistFou = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_hist_uchr.getPath(), iin);
        try {
            historySupervisorRecordFou = jdbcTemplate.query(sqlHistFou, mapper::mapRowToHistorySupervisor);
            log.info("history founder: {}", historySupervisorRecordFou);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No учредитель found for IIN: {}", iin);
        }

        makeHistory(supervisorRecordSup, historySupervisorRecordSup);
        makeHistory(supervisorRecordFou, historySupervisorRecordFou);

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

    private List<SupervisorRecord> makeHistory(List<SupervisorRecord> supervisorRecords, List<HistorySupervisorRecord> historySupervisorRecords){
        List<String> historicalIinBins = historySupervisorRecords.stream()
                .map(HistorySupervisorRecord::getIinBin)
                .toList();

        supervisorRecords.forEach(record -> {
            if (historicalIinBins.contains(record.getIin_bin())) {
                String currentPositionType = record.getPositionType();
                if (currentPositionType != null && !currentPositionType.startsWith("Исторический ")) {
                    record.setPositionType("Исторический " + currentPositionType);
                }
            }
        });

        return supervisorRecords;
    }

    private List<CompanyRecord> getCompanyInfo(String iin) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch company info");
            return null;
        }
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.UL_zt_all.getPath(), iin);
        try {
            List<CompanyRecord> companyRecords = jdbcTemplate.query(sql, mapper::mapRowToCompany);
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

    private List<EsfOverall> getESFInfo(String iin, String dateFrom, String dateTo) throws IOException {
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Overall_esf_overall.getPath(),
                iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESFOverall);
    }

    private List<String> getStatuses(String iin) throws IOException {
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Status_status.getPath(), iin);
        List<StatusRecord> statusRecords = jdbcTemplate.query(sql, mapper::mapRowToStatus);

        return statusRecords.stream().map(StatusRecord::getStatus).toList();
    }


    private List<SupervisorRecord> keepDistinctSupervisorRecords(List<SupervisorRecord> supervisorRecords) {
        return supervisorRecords.stream().distinct().toList();
    }


}
