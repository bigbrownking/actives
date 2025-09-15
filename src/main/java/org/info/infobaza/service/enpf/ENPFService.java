package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.job.BankTurnoverGroup;
import org.info.infobaza.dto.response.job.Pension;
import org.info.infobaza.dto.response.job.Turnover;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.NumberConverter;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ENPFService implements InformationalService {

    private final NumberConverter numberConverter;
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
        List<InformationRecordDt> informationRecords;
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-M-yyyy");

        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.Pension_pension.getPath(), iin, dateFrom, dateTo);
        try {
            informationRecords = jdbcTemplate.query(sql, mapper::mapRowToPension);
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

            String formattedStartDate = periodStartDate.format(outputFormatter);
            String formattedEndDate = periodEndDate.format(outputFormatter);

            Pension pension = new Pension(
                    formattedStartDate,
                    formattedEndDate,
                    entry.getValue().get(0).getDopinfo(),
                    entry.getKey()
            );
            pensions.add(pension);
        }

        pensions.sort(Comparator.comparing(p -> LocalDate.parse(p.getDateFrom(), outputFormatter)));
        return pensions;
    }

    public List<BankTurnoverGroup> getTurnover(String iin) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        List<TurnoverRecord> turnoverRecords;
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Bank_bank.getPath(), iin);

        try {
            turnoverRecords = jdbcTemplate.query(sql, mapper::mapRowToTurnover);
            turnoverRecords.forEach(record -> record.setSumm(numberConverter.formatNumber(record.getSumm())));
        } catch (Exception e) {
            log.error("Error fetching turnover records for IIN: {}", iin, e);
            throw new IOException("Failed to fetch turnover records", e);
        }

        return turnoverRecords.stream()
                .filter(record -> record.getBankName() != null)
                .collect(Collectors.groupingBy(TurnoverRecord::getBankName))
                .entrySet().stream()
                .map(entry -> BankTurnoverGroup.builder()
                        .bankName(entry.getKey())
                        .count(entry.getValue().size())
                        .records(entry.getValue())
                        .build()
                )
                .collect(Collectors.toList());
    }
}
