package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.job.BankTurnoverGroup;
import org.info.infobaza.dto.response.job.Pension;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.NumberConverter;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.info.infobaza.util.date.DateUtil;
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

    private final NumberConverter numberConverter;
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final DateUtil dateUtil;
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

            String formattedStartDate = dateUtil.formatOutput(periodStartDate);
            String formattedEndDate = (i == sortedWorkplaces.size() - 1) ? "по н.в." : dateUtil.formatOutput(periodEndDate);

            double maxSalary = workplaceRecords.stream()
                    .map(InformationRecordDt::getSumm)
                    .filter(Objects::nonNull)
                    .mapToDouble(amount -> {
                        try {
                            return Double.parseDouble(amount) * 10;
                        } catch (NumberFormatException e) {
                            log.warn("Invalid AMOUNT format: {}, defaulting to 0", amount);
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);

            Double lastSalary = (Double) workplaceRecords.stream()
                    .filter(record -> record.getSumm() != null)
                    .max(Comparator.comparing(InformationRecordDt::getDate))
                    .map(record -> {
                        try {
                            return Double.parseDouble(record.getSumm()) * 10;
                        } catch (NumberFormatException e) {
                            log.warn("Invalid AMOUNT format for last record: {}, defaulting to 0", record.getSumm());
                            return 0;
                        }
                    })
                    .orElse(0);
            double sumSalary = workplaceRecords.stream()
                    .map(InformationRecordDt::getSumm)
                    .filter(Objects::nonNull)
                    .mapToDouble(amount -> {
                        try {
                            return Double.parseDouble(amount) * 10;
                        } catch (NumberFormatException e) {
                            log.warn("Invalid AMOUNT format: {}, defaulting to 0", amount);
                            return 0;
                        }
                    })
                    .sum();
            Pension pension = new Pension(
                    formattedStartDate,
                    formattedEndDate,
                    entry.getValue().get(0).getDopinfo(),
                    entry.getKey(),
                    numberConverter.formatNumber(maxSalary),
                    numberConverter.formatNumber(lastSalary),
                    numberConverter.formatNumber(sumSalary)
            );
            pensions.add(pension);
        }

        pensions.sort(Comparator.comparing(p -> dateUtil.parseOutputDate(p.getDateFrom()), Comparator.nullsLast(LocalDate::compareTo)));        return pensions;
    }

    public List<BankTurnoverGroup> getTurnover(String iin) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        List<TurnoverRecord> turnoverRecords;
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Bank_bank.getPath(), iin);

        try {
            turnoverRecords = jdbcTemplate.query(sql, mapper::mapRowToTurnover);
            turnoverRecords.forEach(record -> {
                record.setSumm(numberConverter.formatNumber(record.getSumm()));

                record.setStartDate(dateUtil.formatTimeToCustom(record.getStartDate()));
                record.setEndDate(dateUtil.formatTimeToCustom(record.getEndDate()));
            });
        } catch (Exception e) {
            log.error("Error fetching turnover records for IIN: {}", iin, e);
            throw new IOException("Failed to fetch turnover records", e);
        }

        return turnoverRecords.stream()
                .filter(record -> record.getBankName() != null)
                .collect(Collectors.groupingBy(TurnoverRecord::getBankName))
                .entrySet().stream()
                .map(entry -> {
                    List<TurnoverRecord> records = entry.getValue().stream()
                            .sorted(Comparator.comparing(record -> {
                                try {
                                    return Double.parseDouble(record.getSumm().replaceAll("[^0-9.]", ""));
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid sum format for record: {}, defaulting to 0", record.getSumm());
                                    return 0.0;
                                }
                            }, Comparator.reverseOrder()))
                            .collect(Collectors.toList());

                    double totalSum = records.stream()
                            .mapToDouble(record -> {
                                try {
                                    return Double.parseDouble(record.getSumm().replaceAll("[^0-9.]", ""));
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid sum format for total: {}, defaulting to 0", record.getSumm());
                                    return 0.0;
                                }
                            })
                            .sum();

                    return BankTurnoverGroup.builder()
                            .bankName(entry.getKey())
                            .count(records.size())
                            .records(records)
                            .totalSum(numberConverter.formatNumber(String.valueOf(totalSum)))
                            .build();
                })
                .sorted(Comparator.comparing(group -> {
                    try {
                        return Double.parseDouble(group.getTotalSum().replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid total sum format for group: {}, defaulting to 0", group.getTotalSum());
                        return 0.0;
                    }
                }, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }



    public List<TurnoverRecord> getTurnoverRecords(String iin) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        List<TurnoverRecord> turnoverRecords;
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Bank_bank.getPath(), iin);

        try {
            turnoverRecords = jdbcTemplate.query(sql, mapper::mapRowToTurnover);
            turnoverRecords.forEach(record -> {
                record.setSumm(numberConverter.formatNumber(record.getSumm()));

                record.setStartDate(dateUtil.formatTimeToCustom(record.getStartDate()));
                record.setEndDate(dateUtil.formatTimeToCustom(record.getEndDate()));
            });
        } catch (Exception e) {
            log.error("Error fetching turnover records for IIN: {}", iin, e);
            throw new IOException("Failed to fetch turnover records", e);
        }

        return turnoverRecords;
    }
}
