package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.job.Pension;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ENPFService implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            source = {"ЕНПФ"},
            vids = {"Доход по данным ЕНПФ"},
            isIncome = true
    )
    public List<InformationRecordDt> getENPF(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching ENPF ENPF for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕНПФ_Доход_по_данным_ЕНПФ.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

    public List<Pension> getPension(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        if (dateFrom == null || dateTo == null) {
            throw new IllegalArgumentException("dateFrom and dateTo cannot be null");
        }

        List<Pension> pensions = new ArrayList<>();
        String sql = "SELECT * FROM pfr_dashboard.imp_pension_fl_contr WHERE IIN = ? AND KNP = '010' AND PAY_DATE BETWEEN ? AND ? ORDER BY PAY_DATE DESC";
        List<InformationRecordDt> informationRecords;
        try {
            informationRecords = jdbcTemplate.query(
                    sql,
                    new Object[]{iin, dateFrom, dateTo},
                    mapper::mapRowToPension
            );
        } catch (Exception e) {
            log.error("Error fetching pension records for IIN: {}, dateFrom: {}, dateTo: {}", iin, dateFrom, dateTo, e);
            throw new IOException("Failed to fetch pension records", e);
        }

        if (informationRecords == null || informationRecords.isEmpty()) {
            log.warn("No pension records found for IIN: {}, dateFrom: {}, dateTo: {}", iin, dateFrom, dateTo);
            return pensions;
        }

        Map<String, List<InformationRecordDt>> recordsByWorkplace = informationRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null)
                .sorted(Comparator.comparing(InformationRecordDt::getDate))
                .collect(Collectors.groupingBy(
                        record -> record.getOper() != null ? record.getOper() : "Unknown"
                ));

        List<Map.Entry<String, List<InformationRecordDt>>> sortedWorkplaces = recordsByWorkplace.entrySet().stream()
                .sorted((e1, e2) -> {
                    LocalDate date1 = e1.getValue().stream()
                            .map(InformationRecordDt::getDate)
                            .filter(Objects::nonNull)
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.MAX);
                    LocalDate date2 = e2.getValue().stream()
                            .map(InformationRecordDt::getDate)
                            .filter(Objects::nonNull)
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.MAX);
                    return date1.compareTo(date2);
                })
                .toList();

        for (int i = 0; i < sortedWorkplaces.size(); i++) {
            Map.Entry<String, List<InformationRecordDt>> entry = sortedWorkplaces.get(i);
            List<InformationRecordDt> workplaceRecords = entry.getValue();
            if (workplaceRecords.isEmpty()) continue;

            workplaceRecords.sort(Comparator.comparing(InformationRecordDt::getDate));

            LocalDate periodStartDate = workplaceRecords.get(0).getDate();
            LocalDate periodEndDate;

            if (i < sortedWorkplaces.size() - 1) {
                List<InformationRecordDt> nextWorkplaceRecords = sortedWorkplaces.get(i + 1).getValue();
                periodEndDate = nextWorkplaceRecords.stream()
                        .map(InformationRecordDt::getDate)
                        .filter(Objects::nonNull)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.parse(dateTo));
            } else {
                periodEndDate = LocalDate.parse(dateTo);
            }

            Pension pension = new Pension(
                    periodStartDate.toString(),
                    periodEndDate.toString(),
                    entry.getValue().get(0).getDopinfo(),
                    entry.getKey()

            );
            pensions.add(pension);
        }

        pensions.sort(Comparator.comparing(p -> LocalDate.parse(p.getDateFrom())));
        return pensions;
    }

}
