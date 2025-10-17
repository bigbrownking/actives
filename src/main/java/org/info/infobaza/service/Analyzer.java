package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.Dictionary;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.info.ActiveCountGroup;
import org.info.infobaza.dto.response.info.IinInfo;
import org.info.infobaza.dto.response.info.RecordGroup;
import org.info.infobaza.dto.response.info.active.*;
import org.info.infobaza.dto.response.info.income.IncomeResponse;
import org.info.infobaza.dto.response.info.income.IncomeWithRecords;
import org.info.infobaza.dto.response.info.income.OverallIncome;
import org.info.infobaza.dto.response.info.yearlyCounts.YearlyCount;
import org.info.infobaza.dto.response.info.yearlyCounts.YearlyRecordCounts;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.model.info.active_income.*;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.person.RelationRecord;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.nao_con.NaoConService;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.util.convert.IinChecker;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.NumberConverter;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.info.infobaza.constants.Dictionary.TYPE_PREFIXES;

@Component
@Slf4j
@RequiredArgsConstructor
public class Analyzer {
    private HeadService headService;
    private final PortretService portretService;
    private final JdbcTemplate jdbcTemplate;
    private final Mapper mapper;
    private final SQLFileUtil sqlFileUtil;
    private final IinChecker iinChecker;
    private final NumberConverter numberConverter;
    private final NaoConService naoConService;

    @Autowired
    public void setHeadService(@Lazy HeadService headService) {
        this.headService = headService;
    }

    private static final Map<Method, InformationalService> methodToServiceCache = new ConcurrentHashMap<>();

    static {
        for (InformationalService service : Dictionary.getServiceBeans().values()) {
            for (Method method : service.getClass().getDeclaredMethods()) {
                if (method.getAnnotation(ServiceMetadata.class) != null) {
                    methodToServiceCache.put(method, service);
                }
            }
        }
    }

    private Map<String, List<String>> fetchRelationsForMainIin(String mainIin) {
        try {
            String primarySql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Связи_Семейные.getPath(), mainIin);
            List<RelationRecord> primaryRelations = jdbcTemplate.query(primarySql, mapper::mapRowToPriRelation);

            String secondarySql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Связи_Косвенные.getPath(), mainIin);
            List<RelationRecord> secondaryRelations = jdbcTemplate.query(secondarySql, mapper::mapRowToSecRelation);

            Map<String, List<String>> iinToStatuses = new HashMap<>(primaryRelations.size() + secondaryRelations.size());
            primaryRelations.forEach(rel ->
                    iinToStatuses.computeIfAbsent(rel.getIin_2(), k -> new ArrayList<>()).add(rel.getStatus()));
            secondaryRelations.forEach(rel ->
                    iinToStatuses.computeIfAbsent(rel.getIin_2(), k -> new ArrayList<>()).add(rel.getStatus()));

            return iinToStatuses;
        } catch (Exception e) {
            log.error("Error fetching relations for main IIN: {}", mainIin, e);
            return new HashMap<>();
        }
    }

    private Map<String, List<String>> populateIinToRelation(String iin, List<String> iins) {
        Map<String, List<String>> allIinToRelation = fetchRelationsForMainIin(iin);
        Map<String, List<String>> filteredIinToRelation = new HashMap<>();
        filteredIinToRelation.put(iin, List.of("SELF"));

        if (iins != null) {
            iins.forEach(subIin -> {
                if (allIinToRelation.containsKey(subIin)) {
                    filteredIinToRelation.put(subIin, allIinToRelation.get(subIin));
                }
            });
        }
        return filteredIinToRelation;
    }

    public List<RelationActive> toRelationActives(List<RelationRecord> relationRecords, String dateFrom, String dateTo) {
        return relationRecords.parallelStream()
                .map(rr -> {
                    try {
                        return toRelationActive(rr, dateFrom, dateTo);
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .filter(rr -> !rr.getActives().equals("0"))
                .collect(Collectors.toList());
    }

    private RelationActive toRelationActive(RelationRecord relationRecord, String dateFrom, String dateTo) throws IOException {
        if (relationRecord == null) return null;

        String iin = relationRecord.getIin_2();
        IinInfo iinInfo = portretService.getIinInfo(iin);
        Double totalActives = calculateTotalActivesForPerson(iin, dateFrom, dateTo);
        Double totalIncomes = calculateTotalIncomeByIIN(iin, dateFrom, dateTo);
        List<SupervisorRecord> head = headService.getType(iin);
        Map<String, String> dopinfo = relationRecord.getDopinfo();
        dopinfo.put("vid_sviazi", relationRecord.getVid_sviazi());
        int level = relationRecord.getLevel_rod();
        if(level != 0){
            dopinfo = null;
        }
        return RelationActive.builder()
                .relation(relationRecord.getStatus())
                .fio(iinInfo != null ? iinInfo.getName() : "Unknown")
                .iin(iin)
                .actives(totalActives != null ? numberConverter.formatNumber(totalActives) : "0")
                .incomes(totalIncomes != null ? numberConverter.formatNumber(totalIncomes) : "0")
                .dopinfo(dopinfo)
                //.head(head)
                .level(level)
                .isNominal(portretService.isNominal(iin))
                .build();
    }

    public Double calculateTotalActivesForPerson(String iin, String dateFrom, String dateTo) {
        List<ActiveOverall> allInfoRecords = new ArrayList<>();
        fetchOverallActive(iin, dateFrom, dateTo, allInfoRecords);
        return allInfoRecords.stream()
                .filter(Objects::nonNull)
                .map(ActiveOverall::getSumm)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private void fetchOverallActive(String iin, String dateFrom, String dateTo, List<ActiveOverall> allInfoRecords) {
        try {
            String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ActiveOverall_active_overall.getPath(), iin, dateFrom, dateTo);
            allInfoRecords.addAll(jdbcTemplate.query(sql, mapper::mapRowToActivesOverall));
        } catch (Exception e) {
            log.error("Unexpected error fetching actives for IIN: {}", iin, e);
        }
    }

    public ActiveResponse getAllActivesOfPersonsByDates(String iin, String dateFrom, String dateTo,
                                                        List<String> years, List<String> vids,
                                                        List<String> types, List<String> sources,
                                                        List<String> iins) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();
        List<ESFInformationRecordDt> allEsfRecords = new ArrayList<>();
        List<NaoConRecordDt> naoConRecordDs = new ArrayList<>();


        Map<String, List<String>> filteredIinToRelation = populateIinToRelation(iin, iins);

        List<String> involvedIins = new ArrayList<>(iins != null ? iins.size() + 1 : 1);
        involvedIins.add(iin);
        if (iins != null) involvedIins.addAll(iins);

        Set<String> targetSources = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources)
                : Dictionary.getActiveMethodsBySource().keySet();

        processRecords(involvedIins, dateFrom, dateTo, years, targetSources, vids, types,
                allInfoRecords, allEsfRecords, naoConRecordDs, true);

        ActiveResponse activeResult = toActive(keepDistinctInfo(allInfoRecords),
                keepDistinctEsf(allEsfRecords), keepDistinctNao(naoConRecordDs),
                dateFrom, dateTo, years, involvedIins);
        setIinToRelation(activeResult, filteredIinToRelation);
        return activeResult;
    }

    public IncomeResponse getAllIncomesOfPersonsByDates(String iin, String dateFrom, String dateTo,
                                                        List<String> years, List<String> vids,
                                                        List<String> sources, List<String> iins) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();
        Map<String, List<String>> filteredIinToRelation = populateIinToRelation(iin, iins);

        List<String> involvedIins = new ArrayList<>(iins != null ? iins.size() + 1 : 1);
        involvedIins.add(iin);
        if (iins != null) involvedIins.addAll(iins);

        Set<String> targetSources = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources)
                : Dictionary.getIncomeMethodsBySource().keySet();

        processRecords(involvedIins, dateFrom, dateTo, years, targetSources, vids, null,
                allInfoRecords, null, null, false);

        IncomeResponse incomeResult = toIncome(keepDistinctInfo(allInfoRecords), dateFrom, dateTo, years, involvedIins);
        setIinToRelation(incomeResult, filteredIinToRelation);
        return incomeResult;
    }

    private void processRecords(List<String> iins, String dateFrom, String dateTo, List<String> years,
                                Set<String> sources, List<String> vids, List<String> types,
                                List<InformationRecordDt> infoRecords, List<ESFInformationRecordDt> esfRecords,
                                List<NaoConRecordDt> naoConRecordDs,
                                boolean isActive) {
        List<String> effectiveYears = years != null && !years.isEmpty() ? years : List.of("");

        String mainIin = iins.get(0);
        iins.parallelStream().forEach(currentIin -> {
            boolean isUl = iinChecker.isUl(currentIin);
            for (String year : effectiveYears) {
                String effectiveDateFrom = year.isEmpty() ? dateFrom : year + "-01-01";
                String effectiveDateTo = year.isEmpty() ? dateTo : year + "-12-31";

                for (String source : sources) {
                    List<Method> methods = isActive
                            ? Dictionary.getActiveMethodsBySource().getOrDefault(source, List.of())
                            : Dictionary.getIncomeMethodsBySource().getOrDefault(source, List.of());

                    for (Method method : methods) {
                        ServiceMetadata metadata = method.getAnnotation(ServiceMetadata.class);
                        if ((isActive && metadata.isActive()) || (!isActive && metadata.isIncome())) {
                            if (vids != null && !vids.isEmpty() && Arrays.stream(metadata.vids()).noneMatch(vids::contains))
                                continue;
                            if (types != null && !types.isEmpty() && Arrays.stream(metadata.type()).noneMatch(types::contains))
                                continue;

                            InformationalService service = methodToServiceCache.get(method);
                            if (service == null) {
                                log.error("Service not found for method {}", method.getName());
                                continue;
                            }

                            try {
                                Object result = method.invoke(service, currentIin, effectiveDateFrom, effectiveDateTo);
                                if (result instanceof List<?> resultList && !resultList.isEmpty()) {
                                    Object first = resultList.get(0);
                                    if (isActive && first instanceof ESFInformationRecordDt) {
                                        List<ESFInformationRecordDt> filteredRecords = isUl
                                                ? resultList.stream()
                                                .map(ESFInformationRecordDt.class::cast)
                                                .filter(record -> (currentIin.equals(record.getIin_bin_pokup()) && mainIin.equals(record.getIin_bin_prod())) ||
                                                        (currentIin.equals(record.getIin_bin_prod()) && mainIin.equals(record.getIin_bin_pokup())))
                                                .toList()
                                                : resultList.stream().map(ESFInformationRecordDt.class::cast).toList();
                                        log.debug("ESF records for iin={}, isUl={}: {} records before filter, {} after",
                                                currentIin, isUl, resultList.size(), filteredRecords.size());
                                        synchronized (esfRecords) {
                                            esfRecords.addAll(filteredRecords);
                                        }
                                    } else if (first instanceof InformationRecordDt) {
                                        synchronized (infoRecords) {
                                            infoRecords.addAll((List<InformationRecordDt>) resultList);
                                        }
                                    } else if (first instanceof NaoConRecordDt) {
                                        List<NaoConRecordDt> filteredRecords = isUl
                                                ? resultList.stream()
                                                .map(NaoConRecordDt.class::cast)
                                                .filter(record -> currentIin.equals(record.getIin_bin_pokup()) && mainIin.equals(record.getIin_bin_prod()) ||
                                                        (currentIin.equals(record.getIin_bin_prod()) && mainIin.equals(record.getIin_bin_pokup())))
                                                .toList()
                                                : resultList.stream().map(NaoConRecordDt.class::cast).toList();
                                        log.debug("NaoCon records for iin={}, isUl={}: {} records before filter, {} after",
                                                currentIin, isUl, resultList.size(), filteredRecords.size());
                                        synchronized (naoConRecordDs) {
                                            naoConRecordDs.addAll(filteredRecords);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Failed to invoke method {} for IIN {}: {}", method.getName(), currentIin, e.getMessage());
                                throw new RuntimeException("Failed to invoke service method: " + method.getName(), e);
                            }
                        }
                    }
                }
            }
        });
    }

    private void setIinToRelation(Object result, Map<String, List<String>> filteredIinToRelation) {
        if (result instanceof ActiveWithRecords activeWithRecords) {
            activeWithRecords.setIinToRelation(filteredIinToRelation);
        } else if (result instanceof OverallActive overallActive) {
            overallActive.setIinToRelation(filteredIinToRelation);
        } else if (result instanceof IncomeWithRecords incomeWithRecords) {
            incomeWithRecords.setIinToRelation(filteredIinToRelation);
        } else if (result instanceof OverallIncome overallIncome) {
            overallIncome.setIinToRelation(filteredIinToRelation);
        } else if (result instanceof ActiveCounts activeCounts) {
            activeCounts.setIinToRelation(filteredIinToRelation);
        } else {
            throw new IllegalStateException("Unexpected return type: " + result.getClass().getName());
        }
    }

    private IncomeResponse toIncome(List<InformationRecordDt> informationRecords, String dateFrom,
                                    String dateTo, List<String> years, List<String> iins) {
        boolean isSummaryMode = years == null || years.isEmpty();
        List<InformationRecordDt> filteredRecords = filterAndSortRecords(informationRecords, years, isSummaryMode);

        if (filteredRecords.isEmpty()) {
            return buildEmptyIncomeResponse(dateFrom, dateTo, years, isSummaryMode);
        }

        if (isSummaryMode) {
            return buildOverallIncome(filteredRecords, dateFrom, dateTo, years, iins);
        } else {
            List<RecordDt> recordsByYear = filteredRecords.stream()
                    .map(record -> {
                        try {
                            return InformationRecordDt.builder()
                                    .iin_bin(record.getIin_bin())
                                    .name(portretService.getIinInfo(record.getIin_bin()).getName())
                                    .date(record.getDate())
                                    .database(record.getDatabase())
                                    .oper(record.getOper())
                                    .aktivy(record.getAktivy())
                                    .dopinfo(record.getDopinfo())
                                    .summ(record.getSumm() != null ? numberConverter.formatNumber(record.getSumm()) : null)
                                    .build();
                        } catch (IOException e) {
                            log.error("oops....");
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            return IncomeWithRecords.builder()
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .selectedYears(years)
                    .recordsByYear(new ArrayList<>(recordsByYear))
                    .build();
        }
    }

    private List<InformationRecordDt> filterAndSortRecords
            (List<InformationRecordDt> records, List<String> years, boolean isSummaryMode) {
        return records.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null && record.getOper() != null)
                .filter(record -> isSummaryMode || years.contains(String.valueOf(record.getDate().getYear())))
                .sorted(Comparator.comparing(InformationRecordDt::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .distinct()
                .collect(Collectors.toList());
    }

    private IncomeResponse buildEmptyIncomeResponse(String dateFrom, String dateTo, List<String> years,
                                                    boolean isSummaryMode) {
        log.warn("No income records provided for date range {} to {}, years: {}", dateFrom, dateTo, years);
        return isSummaryMode
                ? OverallIncome.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .selectedYears(years)
                .recordsByYear(new ArrayList<>())
                .build()
                : IncomeWithRecords.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .selectedYears(years)
                .recordsByYear(new ArrayList<>())
                .build();
    }

    private OverallIncome buildOverallIncome(List<InformationRecordDt> filteredRecords, String dateFrom,
                                             String dateTo, List<String> years, List<String> iins) {
        Map<String, List<InformationRecordDt>> groupedByYear = filteredRecords.stream()
                .collect(Collectors.groupingBy(record -> String.valueOf(record.getDate().getYear())));

        Map<String, String> iinToFio = buildIinToFioMap(filteredRecords, iins);

        List<RecordGroup> recordsByYear = groupedByYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildRecordGroup(entry, iinToFio, iins))
                .toList();

        return OverallIncome.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .selectedYears(years)
                .recordsByYear(new ArrayList<>(recordsByYear))
                .build();
    }

    private Map<String, String> buildIinToFioMap(List<InformationRecordDt> records, List<String> iins) {
        Set<String> uniqueIins = records.stream()
                .map(InformationRecordDt::getIin_bin)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (iins != null) uniqueIins.addAll(iins);

        Map<String, String> iinToFio = new HashMap<>(uniqueIins.size());
        uniqueIins.forEach(iin -> {
            IinInfo iinInfo = null;
            try {
                iinInfo = portretService.getIinInfo(iin);
            } catch (IOException e) {
            }
            iinToFio.put(iin, iinInfo != null ? iinInfo.getName() : "Unknown");
        });
        return iinToFio;
    }

    private RecordGroup buildRecordGroup(Map.Entry<String, List<InformationRecordDt>> entry,
                                         Map<String, String> iinToFio, List<String> iins) {
        String year = entry.getKey();
        List<InformationRecordDt> records = entry.getValue();

        String concatenatedOper = records.stream()
                .map(InformationRecordDt::getOper)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));

        double totalSum = records.stream()
                .mapToDouble(record -> parseDoubleOrZero(record.getSumm(), record.getIin_bin(), year))
                .sum();

        String iinsInvolved = records.stream()
                .map(InformationRecordDt::getIin_bin)
                .filter(Objects::nonNull)
                .distinct()
                .filter(iin -> iins != null ? iins.contains(iin) || iin.equals(records.get(0).getIin_bin()) : true)
                .sorted()
                .collect(Collectors.joining(", "));

        Map<String, Double> iinSums = records.stream()
                .filter(record -> record.getIin_bin() != null)
                .collect(Collectors.groupingBy(
                        InformationRecordDt::getIin_bin,
                        Collectors.summingDouble(record -> parseDoubleOrZero(record.getSumm(), record.getIin_bin(), year))
                ));

        StringBuilder info = buildInfoString(iinSums, iinToFio, iins, records.get(0).getIin_bin());

        return RecordGroup.builder()
                .year(year)
                .oper(concatenatedOper)
                .totalSum(numberConverter.formatNumber(totalSum))
                .iinsInvolved(iinsInvolved.isEmpty() ? null : iinsInvolved)
                .info(!info.isEmpty() ? info.toString() : null)
                .build();
    }

    private double parseDoubleOrZero(String summ, String iin, String oper) {
        if (summ == null || summ.trim().isEmpty()) {
            log.warn("Null or empty summ for iin_bin={}, oper={}", iin, oper);
            return 0.0;
        }
        try {
            String cleanedSumm = summ.replaceAll("[^0-9.-]", "");
            return Double.parseDouble(cleanedSumm);
        } catch (NumberFormatException e) {
            log.error("Invalid summ value '{}' for iin_bin={}, oper={}: {}", summ, iin, oper, e.getMessage());
            return 0.0;
        }
    }

    private StringBuilder buildInfoString(Map<String, Double> iinSums, Map<String, String> iinToFio,
                                          List<String> iins, String mainIin) {
        Set<String> processedIins = new HashSet<>();
        StringBuilder info = new StringBuilder();

        if (mainIin != null && iinSums.size() > 1 || (iins != null && !iins.isEmpty())) {
            if (mainIin != null && iinSums.containsKey(mainIin) && processedIins.add(mainIin)) {
                String mainFio = iinToFio.get(mainIin);
                double mainSum = iinSums.getOrDefault(mainIin, 0.0);
                info.append("Сумма ").append(mainFio).append(": ").append(numberConverter.formatNumber(mainSum)).append('\n');
            }

            if (iins != null) {
                iins.forEach(subIin -> {
                    if (iinSums.containsKey(subIin) && processedIins.add(subIin)) {
                        String subFio = iinToFio.get(subIin);
                        double subSum = iinSums.getOrDefault(subIin, 0.0);
                        info.append(" Сумма ").append(subFio).append(": ").append(numberConverter.formatNumber(subSum)).append('\n');
                    }
                });
            }
        }
        return info;
    }

    public ActiveResponse toActive(List<InformationRecordDt> informationRecords,
                                   List<ESFInformationRecordDt> esfInformationRecords,
                                   List<NaoConRecordDt> naoConRecords,
                                   String dateFrom, String dateTo, List<String> years, List<String> iins) {
        boolean isSummaryMode = years == null || years.isEmpty();
        List<RecordDt> allRecords = Stream.of(
                        informationRecords == null ? Collections.<RecordDt>emptyList() : informationRecords,
                        esfInformationRecords == null ? Collections.<RecordDt>emptyList() : esfInformationRecords,
                        naoConRecords == null ? Collections.<RecordDt>emptyList() : naoConRecords
                )
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null && record.getOper() != null)
                .filter(record -> isSummaryMode || years.contains(String.valueOf(record.getDate().getYear())))
                .sorted(Comparator.comparing(RecordDt::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .distinct()
                .collect(Collectors.toList());

        log.info("ALL RECORDS SIZE: {}", allRecords.size());
        List<RecordDt> deduplicatedRecords = deduplicateRecords(allRecords);
        if (deduplicatedRecords.isEmpty()) {
            log.warn("No records provided for date range {} to {}, years: {}", dateFrom, dateTo, years);
            return isSummaryMode
                    ? OverallActive.builder().dateFrom(dateFrom).dateTo(dateTo).selectedYears(years).recordsByOper(new ArrayList<>()).build()
                    : ActiveWithRecords.builder().dateFrom(dateFrom).dateTo(dateTo).selectedYears(years).recordsByOper(new ArrayList<>()).build();
        }
        log.info("AFTER DEDUPLICATION: {}", deduplicatedRecords.size());

        List<RecordDt> formattedRecords = formatRecords(deduplicatedRecords);

        if (isSummaryMode) {
            Map<String, List<RecordDt>> recordsByOper = groupRecordsByOper(formattedRecords);
            return buildOverallActive(recordsByOper, dateFrom, dateTo, years, iins);
        } else {
            return ActiveWithRecords.builder()
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .selectedYears(years)
                    .recordsByOper(new ArrayList<>(formattedRecords))
                    .build();
        }
    }

    private List<RecordDt> deduplicateRecords(List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        List<RecordDt> result = new ArrayList<>();

        for (RecordDt currentRecord : records) {
            boolean isDuplicate = false;

            for (RecordDt existingRecord : result) {
                if (areDuplicates(currentRecord, existingRecord)) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                result.add(currentRecord);
            }
        }

        return result;
    }

    private boolean areDuplicates(RecordDt record1, RecordDt record2) {
        if (Objects.equals(record1.getDatabase(), record2.getDatabase())) {
            return false;
        }

        boolean sameIin = isSameIin(record1, record2);

        boolean sameSumm = Objects.equals(record1.getSumm(), record2.getSumm());

        boolean sameAktyvy = isSameAktyvy(record1, record2);

        boolean sameOrCloseDate = isDateWithinTwoDays(record1.getDate(), record2.getDate());

        return sameIin && sameSumm && sameAktyvy && sameOrCloseDate;
    }

    private boolean isSameAktyvy(RecordDt record1, RecordDt record2) {
        String aktyvy1 = getAktyvyValue(record1);
        String aktyvy2 = getAktyvyValue(record2);


        return Objects.equals(aktyvy1, aktyvy2);
    }

    private String getAktyvyValue(RecordDt record) {
        if (record instanceof InformationRecordDt infoRecord) {
            return infoRecord.getAktivy();
        } else if (record instanceof ESFInformationRecordDt esfRecord) {
            return esfRecord.getAktivy();
        }
        return null;
    }

    private boolean isSameIin(RecordDt record1, RecordDt record2) {
        if (record1 instanceof ESFInformationRecordDt esf1 && record2 instanceof ESFInformationRecordDt esf2) {

            return (Objects.equals(esf1.getIin_bin(), esf2.getIin_bin()) &&
                    Objects.equals(esf1.getIin_bin_pokup(), esf2.getIin_bin_pokup()) &&
                    Objects.equals(esf1.getIin_bin_prod(), esf2.getIin_bin_prod()));
        }

        if (record1 instanceof ESFInformationRecordDt esf && record2 instanceof InformationRecordDt info) {

            return Objects.equals(esf.getIin_bin(), info.getIin_bin()) &&
                    Objects.equals(esf.getIin_bin_pokup(), info.getIin_bin()) &&
                    Objects.equals(esf.getIin_bin_prod(), info.getIin_bin());
        }

        if (record1 instanceof InformationRecordDt info && record2 instanceof ESFInformationRecordDt esf) {
            return Objects.equals(esf.getIin_bin(), info.getIin_bin()) &&
                    Objects.equals(esf.getIin_bin_pokup(), info.getIin_bin()) &&
                    Objects.equals(esf.getIin_bin_prod(), info.getIin_bin());
        }

        return Objects.equals(record1.getIin_bin(), record2.getIin_bin());
    }

    private boolean isDateWithinTwoDays(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return Objects.equals(date1, date2);
        }

        long daysDifference = Math.abs(ChronoUnit.DAYS.between(date1, date2));
        return daysDifference <= 2;
    }

    private Map<String, List<RecordDt>> groupRecordsByOper(List<RecordDt> allRecords) {
        Map<String, List<RecordDt>> recordsByOper = new HashMap<>();
        List<String> buyOperations = List.of("Наличие", "Приобретение");

        allRecords.forEach(record -> {
            String oper = buyOperations.contains(record.getOper()) ? "Наличие и приобретение" : record.getOper();
            recordsByOper.computeIfAbsent(oper, k -> new ArrayList<>()).add(record);
        });

        recordsByOper.computeIfAbsent("Наличие и приобретение", k -> new ArrayList<>());
        recordsByOper.computeIfAbsent("Реализация", k -> new ArrayList<>());
        return recordsByOper;
    }

    private OverallActive buildOverallActive(Map<String, List<RecordDt>> recordsByOper,
                                             String dateFrom, String dateTo, List<String> years, List<String> iins) {
        List<RecordGroup> aggregatedRecords = new ArrayList<>();
        Map<String, String> iinToFio = buildIinToFioMapForActive(recordsByOper, iins);

        recordsByOper.forEach((oper, records) -> {
            Map<String, Map<String, Double>> yearIinSums = records.stream()
                    .collect(Collectors.groupingBy(
                            record -> String.valueOf(record.getDate().getYear()),
                            Collectors.groupingBy(
                                    record -> record.getIin_bin() != null ? record.getIin_bin() : "Unknown",
                                    Collectors.summingDouble(record -> parseDoubleOrZero(record.getSumm(), record.getIin_bin(), oper))
                            )
                    ));

            List<RecordGroup> recordGroups = yearIinSums.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> buildActiveRecordGroup(entry, oper, iinToFio, iins, records))
                    .toList();

            aggregatedRecords.addAll(new ArrayList<>(recordGroups));
        });

        return OverallActive.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .selectedYears(years)
                .recordsByOper(aggregatedRecords)
                .build();
    }

    private Map<String, String> buildIinToFioMapForActive
            (Map<String, List<RecordDt>> recordsByOper, List<String> iins) {
        Set<String> uniqueIins = recordsByOper.values().stream()
                .flatMap(List::stream)
                .map(RecordDt::getIin_bin)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (iins != null) uniqueIins.addAll(iins);

        Map<String, String> iinToFio = new HashMap<>(uniqueIins.size());
        uniqueIins.forEach(iin -> {
            IinInfo iinInfo = null;
            try {
                iinInfo = portretService.getIinInfo(iin);
            } catch (IOException e) {
            }
            iinToFio.put(iin, iinInfo != null ? iinInfo.getName() : "Unknown");
        });
        return iinToFio;
    }

    private RecordGroup buildActiveRecordGroup(Map.Entry<String, Map<String, Double>> entry, String oper,
                                               Map<String, String> iinToFio, List<String> iins, List<RecordDt> records) {
        String year = entry.getKey();
        double totalSum = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
        String iinsInvolved = entry.getValue().keySet().stream()
                .filter(iin -> iin != null && !"Unknown".equals(iin))
                .filter(iins != null ? iins::contains : iin -> true)
                .sorted()
                .collect(Collectors.joining(", "));

        Map<String, Double> iinSums = entry.getValue().entrySet().stream()
                .filter(e -> e.getKey() != null && !"Unknown".equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        StringBuilder info = buildInfoString(iinSums, iinToFio, iins, records.stream().findFirst().map(RecordDt::getIin_bin).orElse(null));

        return RecordGroup.builder()
                .year(year)
                .totalSum(numberConverter.formatNumber(totalSum))
                .oper(oper)
                .info(!info.isEmpty() ? info.toString() : null)
                .iinsInvolved(iinsInvolved.isEmpty() ? null : iinsInvolved)
                .build();
    }

    private List<RecordDt> formatRecords(List<RecordDt> records) {
        return records.stream()
                .map(record -> {
                    if (record instanceof InformationRecordDt info) {
                        try {
                            return InformationRecordDt.builder()
                                    .iin_bin(info.getIin_bin())
                                    .name(portretService.getIinInfo(info.getIin_bin()).getName())
                                    .date(info.getDate())
                                    .aktivy(info.getAktivy())
                                    .database(info.getDatabase())
                                    .oper(info.getOper())
                                    .dopinfo(info.getDopinfo())
                                    .summ(info.getSumm() != null ? numberConverter.formatNumber(info.getSumm()) : null)
                                    .build();
                        } catch (IOException e) {
                            log.error("Error formatting InformationRecordDt: {}", e.getMessage());
                        }
                    } else if (record instanceof ESFInformationRecordDt esf) {
                        try {
                            return ESFInformationRecordDt.builder()
                                    .iin_bin(esf.getIin_bin())
                                    .name(portretService.getIinInfo(esf.getIin_bin()).getName())
                                    .iin_bin_pokup(esf.getIin_bin_pokup())
                                    .iin_bin_prod(esf.getIin_bin_prod())
                                    .date(esf.getDate())
                                    .aktivy(esf.getAktivy())
                                    .database(esf.getDatabase())
                                    .oper(esf.getOper())
                                    .dopinfo(esf.getDopinfo())
                                    .num_doc(esf.getNum_doc())
                                    .summ(esf.getSumm() != null ? numberConverter.formatNumber(esf.getSumm()) : null)
                                    .build();
                        } catch (IOException e) {
                            log.error("Error formatting ESFInformationRecordDt: {}", e.getMessage());
                        }
                    }else if (record instanceof NaoConRecordDt nao) {
                        try {
                            return NaoConRecordDt.builder()
                                    .iin_bin(nao.getIin_bin())
                                    .name(portretService.getIinInfo(nao.getIin_bin()).getName())
                                    .iin_bin_pokup(nao.getIin_bin_pokup())
                                    .iin_bin_prod(nao.getIin_bin_prod())
                                    .date(nao.getDate())
                                    .aktivy(nao.getAktivy())
                                    .database(nao.getDatabase())
                                    .oper(nao.getOper())
                                    .dopinfo(nao.getDopinfo())
                                    .num_doc(nao.getNum_doc())
                                    .vid_ned(nao.getVid_ned())
                                    .podrod_vid_ned(nao.getPodrod_vid_ned())
                                    .kd_fixed(nao.getKd_fixed())
                                    .rka(nao.getRka())
                                    .address(nao.getAddress())
                                    .obshaya_ploshad(nao.getObshaya_ploshad())
                                    .summ(nao.getSumm() != null ? numberConverter.formatNumber(nao.getSumm()) : null)
                                    .build();
                        } catch (IOException e) {
                            log.error("Error formatting ESFInformationRecordDt: {}", e.getMessage());
                        }
                    }

                    return record;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String extractTypeFromDopinfo(String aktivy, String dopinfo) {
        if (dopinfo == null || dopinfo.isEmpty() || aktivy == null) {
            return null;
        }

        String[] prefixes = TYPE_PREFIXES.get(aktivy);
        if (prefixes == null) {
            return null;
        }

        for (String prefix : prefixes) {
            Optional<String> result = extractType(dopinfo, prefix);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return null;
    }

    private Optional<String> extractType(String dopinfo, String prefix) {
        int startIndex = dopinfo.indexOf(prefix);
        if (startIndex == -1) {
            return Optional.empty();
        }

        startIndex += prefix.length();
        int endIndex = dopinfo.indexOf(";", startIndex);
        String type = (endIndex != -1)
                ? dopinfo.substring(startIndex, endIndex).trim()
                : dopinfo.substring(startIndex).trim();

        return type.isEmpty() ? Optional.empty() : Optional.of(type);
    }

    public double calculateTotalIncomeByIIN(String iin, String dateFrom, String dateTo) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();
        Set<String> targetSources = Dictionary.getIncomeMethodsBySource().keySet();

        processRecords(Collections.singletonList(iin), dateFrom, dateTo, null, targetSources, null, null,
                allInfoRecords, null, null, false);

        return allInfoRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getSumm() != null)
                .mapToDouble(record -> parseDoubleOrZero(record.getSumm(), record.getIin_bin(), null))
                .sum();
    }

    public YearlyRecordCounts getYearlyRecordCounts(String iin, String dateFrom, String dateTo,
                                                    List<String> sources, List<String> types, List<String> vids,
                                                    List<String> iins, boolean isActive) {
        List<YearlyCount> counts = isActive
                ? calculateActiveYearlyCounts(iin, dateFrom, dateTo, sources, types, vids, iins)
                : calculateIncomeYearlyCounts(iin, dateFrom, dateTo, sources, vids, iins);

        return YearlyRecordCounts.builder()
                .counts(counts)
                .build();
    }

    private List<YearlyCount> calculateActiveYearlyCounts(String iin, String dateFrom, String dateTo,
                                                          List<String> sources, List<String> types,  List<String> vids, List<String> iins) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();
        List<ESFInformationRecordDt> allEsfRecords = new ArrayList<>();
        List<NaoConRecordDt> allNaoConRecords = new ArrayList<>();

        List<String> involvedIins = buildInvolvedIins(iin, iins);
        Set<String> targetSources = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources)
                : Dictionary.getActiveMethodsBySource().keySet();

        processRecords(involvedIins, dateFrom, dateTo, null, targetSources, vids, types,
                allInfoRecords, allEsfRecords, allNaoConRecords, true);

        List<RecordDt> allRecords = Stream.of(
                        allInfoRecords == null ? Collections.<RecordDt>emptyList() : allInfoRecords,
                        allEsfRecords == null ? Collections.<RecordDt>emptyList() : allEsfRecords,
                        allNaoConRecords == null ? Collections.<RecordDt>emptyList() : allNaoConRecords
                )
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null)
                .distinct()
                .collect(Collectors.toList());
        log.info("ALL RECORDS SIZE: {}", allRecords.size());
        List<RecordDt> deduplicated = deduplicateRecords(allRecords);
        log.info("ALL DEDUPLICATED SIZE: {}", deduplicated.size());
        return calculateYearlyCounts(deduplicated);
    }

    private List<YearlyCount> calculateIncomeYearlyCounts(String iin, String dateFrom, String dateTo,
                                                          List<String> sources, List<String> vids, List<String> iins) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();

        List<String> involvedIins = buildInvolvedIins(iin, iins);
        Set<String> targetSources = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources)
                : Dictionary.getIncomeMethodsBySource().keySet();

        processRecords(involvedIins, dateFrom, dateTo, null, targetSources, vids, null,
                allInfoRecords, null, null, false);

        List<RecordDt> allRecords = Stream.of(
                        allInfoRecords == null ? Collections.<RecordDt>emptyList() : allInfoRecords
                )
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null)
                .distinct()
                .collect(Collectors.toList());
        return calculateYearlyCounts(deduplicateRecords(allRecords));
    }

    private List<String> buildInvolvedIins(String iin, List<String> iins) {
        List<String> involvedIins = new ArrayList<>(iins != null ? iins.size() + 1 : 1);
        involvedIins.add(iin);
        if (iins != null) {
            involvedIins.addAll(iins.stream().distinct().filter(subIin -> !subIin.equals(iin)).toList());
        }
        return involvedIins;
    }

    private List<YearlyCount> calculateYearlyCounts(List<? extends RecordDt> records) {
        return records.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null)
                .collect(Collectors.groupingBy(
                        record -> String.valueOf(record.getDate().getYear()),
                        Collector.of(
                                () -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO},
                                (acc, record) -> {
                                    acc[0] = acc[0].add(BigDecimal.ONE);
                                    BigDecimal val = numberConverter.parseBigDecimalOrZero(record.getSumm());
                                    acc[1] = acc[1].add(val);
                                },
                                (acc1, acc2) -> {
                                    acc1[0] = acc1[0].add(acc2[0]);
                                    acc1[1] = acc1[1].add(acc2[1]);
                                    return acc1;
                                },
                                acc -> YearlyCount.builder()
                                        .year(null)
                                        .count(acc[0].intValue())
                                        .summ(numberConverter.formatNumber(acc[1].toPlainString()))
                                        .build()
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    YearlyCount yc = entry.getValue();
                    yc.setYear(entry.getKey());
                    return yc;
                })
                .sorted(Comparator.comparing(YearlyCount::getYear))
                .collect(Collectors.toList());
    }

    public ActiveResponse getAllActiveCountsOfPersonsByDates(String iin, String dateFrom, String dateTo,
                                                             List<String> years, List<String> vids,
                                                             List<String> types, List<String> sources,
                                                             List<String> iins, String button) {
        List<InformationRecordDt> allInfoRecords = new ArrayList<>();
        List<ESFInformationRecordDt> allEsfRecords = new ArrayList<>();
        List<NaoConRecordDt> allNaoRecords = new ArrayList<>();
        Map<String, List<String>> filteredIinToRelation = populateIinToRelation(iin, iins);

        List<String> involvedIins = new ArrayList<>(iins != null ? iins.size() + 1 : 1);
        involvedIins.add(iin);
        if (iins != null) involvedIins.addAll(iins);

        Set<String> targetSources = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources)
                : Dictionary.getActiveMethodsBySource().keySet();

        switch (button) {
            case "Все" -> {
                // Process records and group by database
                Map<String, List<InformationRecordDt>> infoRecordsByDatabase;
                Map<String, List<ESFInformationRecordDt>> esfRecordsByDatabase;

                processRecords(involvedIins, dateFrom, dateTo, years, targetSources, vids, types,
                        allInfoRecords, allEsfRecords, allNaoRecords, true);

                // Group InformationRecordDt by database
                infoRecordsByDatabase = allInfoRecords.stream()
                        .filter(record -> record.getDatabase() != null)
                        .collect(Collectors.groupingBy(InformationRecordDt::getDatabase));

                // Group ESFInformationRecordDt by database
                esfRecordsByDatabase = allEsfRecords.stream()
                        .filter(record -> record.getDatabase() != null)
                        .collect(Collectors.groupingBy(ESFInformationRecordDt::getDatabase));

                // Clear original lists and add back records in database-grouped order
                allInfoRecords.clear();
                allEsfRecords.clear();

                // Sort databases alphabetically to ensure consistent order
                infoRecordsByDatabase.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> allInfoRecords.addAll(entry.getValue()));

                esfRecordsByDatabase.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> allEsfRecords.addAll(entry.getValue()));
            }
            case "Текущий" -> {
                for (String currentIin : involvedIins) {
                    try {
                        List<NaoConRecordDt> naoConRecords = naoConService.getNaoConHouse(currentIin, dateFrom, dateTo);
                        Set<String> kdsWithSale = new HashSet<>();
                        Map<String, NaoConRecordDt> latestRecordsByKd = new HashMap<>();

                        for (NaoConRecordDt record : naoConRecords) {
                            String kd = record.getKd_fixed();
                            if (kd != null) {
                                if ("Реализация".equals(record.getOper())) {
                                    kdsWithSale.add(kd);
                                }
                                latestRecordsByKd.compute(kd, (key, oldRecord) ->
                                        oldRecord == null || isLaterDate(record.getDate().toString(), oldRecord.getDate().toString())
                                                ? record : oldRecord);
                            }
                        }
                        log.info("LAST RECORDS: {}", latestRecordsByKd);

                        List<NaoConRecordDt> filteredRecords = latestRecordsByKd.values().stream()
                                .filter(record -> {
                                    String kd = record.getKd_fixed();
                                    return kd != null && !kdsWithSale.contains(kd) &&
                                            ("Наличие".equals(record.getOper()) || "Приобретение".equals(record.getOper()));
                                })
                                .collect(Collectors.groupingBy(NaoConRecordDt::getDatabase))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .flatMap(entry -> entry.getValue().stream())
                                .toList();

                        synchronized (allNaoRecords) {
                            allNaoRecords.clear();
                            allNaoRecords.addAll(filteredRecords);
                        }
                        log.debug("Filtered {} NaoCon records for IIN {} in 'Текущий' mode", filteredRecords.size(), currentIin);
                    } catch (IOException e) {
                        log.error("Failed to fetch NAO CON records for IIN {}: {}", currentIin, e.getMessage());
                    }
                }
            }
            case "Исторический" -> {
                for (String currentIin : involvedIins) {
                    try {
                        List<NaoConRecordDt> naoConRecords = naoConService.getNaoConHouse(currentIin, dateFrom, dateTo);

                        Map<String, List<NaoConRecordDt>> recordsByKd = naoConRecords.stream()
                                .filter(record -> record.getKd_fixed() != null)
                                .collect(Collectors.groupingBy(NaoConRecordDt::getKd_fixed));

                        List<NaoConRecordDt> pairedRecords = new ArrayList<>();

                        for (Map.Entry<String, List<NaoConRecordDt>> entry : recordsByKd.entrySet()) {
                            List<NaoConRecordDt> kdRecords = entry.getValue();

                            boolean hasBuy = kdRecords.stream()
                                    .anyMatch(r -> "Приобретение".equals(r.getOper()));
                            boolean hasSell = kdRecords.stream()
                                    .anyMatch(r -> "Реализация".equals(r.getOper()));

                            kdRecords.sort(Comparator.comparing(r -> r.getDate().toString()));

                            if (hasBuy && hasSell) {
                                pairedRecords.addAll(kdRecords);
                            }
                            else if (!hasBuy && hasSell) {
                                pairedRecords.addAll(kdRecords);
                            }
                        }

                        Map<String, List<NaoConRecordDt>> naoRecordsByDatabase = pairedRecords.stream()
                                .filter(record -> record.getDatabase() != null)
                                .collect(Collectors.groupingBy(NaoConRecordDt::getDatabase));

                        synchronized (allNaoRecords) {
                            allNaoRecords.clear();
                            naoRecordsByDatabase.entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .forEach(entry -> allNaoRecords.addAll(entry.getValue()));
                        }

                        log.debug("Added {} paired NaoCon records for IIN {} in 'Исторический' mode", pairedRecords.size(), currentIin);

                    } catch (IOException e) {
                        log.error("Failed to fetch NAO CON records for IIN {}: {}", currentIin, e.getMessage());
                    }
                }
            }
        }

        ActiveResponse activeResult = toActiveCount(keepDistinctInfo(allInfoRecords),
                keepDistinctEsf(allEsfRecords), keepDistinctNao(allNaoRecords),
                dateFrom, dateTo, years, involvedIins);
        setIinToRelation(activeResult, filteredIinToRelation);
        return activeResult;
    }

    private boolean isLaterDate(String date1, String date2) {
        if (date1 == null || date2 == null) return date1 != null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(date1).after(sdf.parse(date2));
        } catch (ParseException e) {
            log.warn("Failed to parse dates {} or {}: {}", date1, date2, e.getMessage());
            return false;
        }
    }

    private String extractKdFromDopinfo(String dopinfo) {
        if (dopinfo == null || dopinfo.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("kd:([^;]+)");
        Matcher matcher = pattern.matcher(dopinfo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public ActiveResponse toActiveCount(List<InformationRecordDt> informationRecords,
                                        List<ESFInformationRecordDt> esfInformationRecords,
                                        List<NaoConRecordDt> naoConRecordDs,
                                        String dateFrom, String dateTo, List<String> years, List<String> iins) {
        boolean isSummaryMode = years == null || years.isEmpty();
        List<RecordDt> allRecords = Stream.of(
                        informationRecords == null ? Collections.<RecordDt>emptyList() : informationRecords,
                        esfInformationRecords == null ? Collections.<RecordDt>emptyList() : esfInformationRecords,
                        naoConRecordDs == null ? Collections.<RecordDt>emptyList() : naoConRecordDs
                )
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(record -> record.getDate() != null && record.getOper() != null)
                .filter(record -> isSummaryMode || years.contains(String.valueOf(record.getDate().getYear())))
                .sorted(Comparator.comparing(RecordDt::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .distinct()
                .collect(Collectors.toList());

        log.info("ALL RECORDS SIZE: {}", allRecords.size());

        List<RecordDt> formattedRecords = allRecords.stream()
                .peek(record -> {
                    if (record instanceof NaoConRecordDt naoRecord) {
                        naoRecord.setDopinfo(null);
                    }
                    record.setSumm(numberConverter.formatNumber(record.getSumm()));
                })
                .toList();

        List<RecordDt> deduplicatedRecords = deduplicateRecords(formattedRecords);
        if (deduplicatedRecords.isEmpty()) {
            log.warn("No records provided for date range {} to {}, years: {}", dateFrom, dateTo, years);
            return ActiveCounts.builder()
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .selectedYears(years)
                    .aktivyTypeCounts(new ArrayList<>())
                    .build();
        }
        log.info("AFTER DEDUPLICATION: {}", deduplicatedRecords.size());

        List<ActiveCountGroup> aktivyTypeCounts = deduplicatedRecords.stream()
                .filter(record -> record.getDatabase() != null && record.getAktivy() != null)
                .collect(Collectors.groupingBy(
                        record -> {
                            String type = extractTypeFromDopinfo(record.getAktivy(), record.getDopinfo());
                            return String.format("%s|%s|%s",
                                    record.getDatabase(),
                                    record.getAktivy(),
                                    type != null && !type.isEmpty() ? type : "");
                        }
                ))
                .entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    return ActiveCountGroup.builder()
                            .database(parts[0])
                            .aktivy(parts[1])
                            .type(parts.length > 2 ? parts[2] : "")
                            .count(entry.getValue().size())
                            .records(entry.getValue())
                            .build();
                })
                .sorted(Comparator.comparing(ActiveCountGroup::getDatabase))
                .collect(Collectors.toList());

        return ActiveCounts.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .selectedYears(years)
                .aktivyTypeCounts(aktivyTypeCounts)
                .build();
    }

    public static List<InformationRecordDt> keepDistinctInfo(List<InformationRecordDt> informationRecords) {
        return informationRecords.stream().distinct().collect(Collectors.toList());
    }

    public static List<ESFInformationRecordDt> keepDistinctEsf(List<ESFInformationRecordDt> esfInformationRecords) {
        return esfInformationRecords.stream().distinct().collect(Collectors.toList());
    }
    public static List<NaoConRecordDt> keepDistinctNao(List<NaoConRecordDt> naoConRecordDs) {
        return naoConRecordDs.stream().distinct().collect(Collectors.toList());
    }

    public static List<RelationRecord> keepDistinctRelations(List<RelationRecord> relationRecords) {
        return relationRecords.stream().distinct().collect(Collectors.toList());
    }
}