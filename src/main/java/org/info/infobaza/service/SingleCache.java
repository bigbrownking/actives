package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.info.active.SingleIinActiveResult;
import org.info.infobaza.dto.response.info.income.SingleIinIncomeResult;
import org.info.infobaza.dto.response.info.yearlyCounts.SingleIinYearlyCounts;
import org.info.infobaza.dto.response.info.yearlyCounts.YearlyCount;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.util.RecordKeyGenerator;
import org.info.infobaza.util.convert.IinChecker;
import org.info.infobaza.util.convert.NumberConverter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleCache {
    private final FetcherRegistry fetcherRegistry;
    private final NumberConverter numberConverter;

    @Cacheable(
            value = "activeBySingleIin",
            key = "#iin + '_' + #dateFrom + '_' + #dateTo + '_' + " +
                    "T(java.util.Objects).hash(#years) + '_' + " +
                    "T(java.util.Objects).hash(#vids) + '_' + " +
                    "T(java.util.Objects).hash(#types) + '_' + " +
                    "T(java.util.Objects).hash(#sources)"
    )
    public SingleIinActiveResult fetchActiveForSingleIin(
            String iin,
            String dateFrom,
            String dateTo,
            List<String> years,
            List<String> vids,
            List<String> types,
            Set<String> sources) {

        List<InformationRecordDt> infoRecords = new ArrayList<>();
        List<ESFInformationRecordDt> esfRecords = new ArrayList<>();
        List<NaoConRecordDt> naoRecords = new ArrayList<>();

        processRecords(
                Collections.singletonList(iin),
                dateFrom,
                dateTo,
                years != null ? years : List.of(),
                sources,
                vids,
                types,
                infoRecords,
                esfRecords,
                naoRecords,
                true
        );

        return new SingleIinActiveResult(infoRecords, esfRecords, naoRecords);
    }


    @Cacheable(
            value = "incomeBySingleIin",
            key = "#iin + '_' + #dateFrom + '_' + #dateTo + '_' + " +
                    "T(java.util.Objects).hash(#years) + '_' + " +
                    "T(java.util.Objects).hash(#vids) + '_' + " +
                    "T(java.util.Objects).hash(#sources)"
    )
    public SingleIinIncomeResult fetchIncomeForSingleIin(
            String iin,
            String dateFrom,
            String dateTo,
            List<String> years,
            List<String> vids,
            Set<String> sources) {

        List<InformationRecordDt> infoRecords = new ArrayList<>();

        processRecords(
                Collections.singletonList(iin),
                dateFrom,
                dateTo,
                years != null ? years : List.of(),
                sources,
                vids,
                null,
                infoRecords,
                null,
                null,
                false
        );

        return new SingleIinIncomeResult(infoRecords);
    }


    @Cacheable(
            value = "yearCountActiveByIin",
            key = "#iin + '_' + #dateFrom + '_' + #dateTo + '_' + " +
                    "T(java.util.Objects).hash(#vids) + '_' + " +
                    "T(java.util.Objects).hash(#types) + '_' + " +
                    "T(java.util.Objects).hash(#sources)"
    )
    public SingleIinYearlyCounts fetchActiveYearlyCountsForSingleIin(
            String iin,
            String dateFrom,
            String dateTo,
            List<String> vids,
            List<String> types,
            Set<String> sources) {

        List<InformationRecordDt> info = new ArrayList<>();
        List<ESFInformationRecordDt> esf = new ArrayList<>();
        List<NaoConRecordDt> nao = new ArrayList<>();

        processRecords(
                Collections.singletonList(iin),
                dateFrom,
                dateTo,
                null,
                sources,
                vids,
                types,
                info,
                esf,
                nao,
                true
        );

        List<RecordDt> allRecords = Stream.of(info, esf, nao)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(r -> r.getDate() != null)
                .distinct()
                .collect(Collectors.toList());

        List<RecordDt> deduplicated = deduplicateRecords(allRecords);
        List<YearlyCount> counts = calculateYearlyCounts(deduplicated);

        return new SingleIinYearlyCounts(counts);
    }

    @Cacheable(
            value = "yearCountIncomeByIin",
            key = "#iin + '_' + #dateFrom + '_' + #dateTo + '_' + " +
                    "T(java.util.Objects).hash(#vids) + '_' + " +
                    "T(java.util.Objects).hash(#sources)"
    )
    public SingleIinYearlyCounts fetchIncomeYearlyCountsForSingleIin(
            String iin,
            String dateFrom,
            String dateTo,
            List<String> vids,
            Set<String> sources) {

        List<InformationRecordDt> info = new ArrayList<>();

        processRecords(
                Collections.singletonList(iin),
                dateFrom,
                dateTo,
                null,
                sources,
                vids,
                null,
                info,
                null,
                null,
                false
        );

        List<RecordDt> allRecords = info.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getDate() != null)
                .distinct()
                .collect(Collectors.toList());

        List<YearlyCount> counts = calculateYearlyCounts(deduplicateRecords(allRecords));

        return new SingleIinYearlyCounts(counts);
    }

    public void processRecords(List<String> iins, String dateFrom, String dateTo, List<String> years,
                                Set<String> sources, List<String> vids, List<String> types,
                                List<InformationRecordDt> infoRecords,
                                List<ESFInformationRecordDt> esfRecords,
                                List<NaoConRecordDt> naoConRecordDs,
                                boolean isActive) {

        Set<String> esfKeys = Collections.synchronizedSet(new HashSet<>());
        Set<String> infoKeys = Collections.synchronizedSet(new HashSet<>());
        Set<String> naoConKeys = Collections.synchronizedSet(new HashSet<>());

        List<String> effectiveYears = years != null && !years.isEmpty() ? years : List.of("");
        String mainIin = iins.get(0);

        iins.forEach(currentIin -> {
            boolean isUl = IinChecker.isUl(currentIin);

            for (String year : effectiveYears) {
                String effectiveDateFrom = year.isEmpty() ? dateFrom : year + "-01-01";
                String effectiveDateTo = year.isEmpty() ? dateTo : year + "-12-31";

                for (String source : sources) {
                    List<DataFetcher> fetchers = isActive
                            ? fetcherRegistry.getActiveFetchers(source)
                            : fetcherRegistry.getIncomeFetchers(source);

                    for (DataFetcher fetcher : fetchers) {
                        FetcherMetadata meta = fetcher.getMetadata();

                        if (vids != null && !vids.isEmpty() &&
                                Arrays.stream(meta.getVids()).noneMatch(vids::contains)) continue;
                        if (types != null && !types.isEmpty() &&
                                Arrays.stream(meta.getTypes()).noneMatch(types::contains)) continue;

                        try {
                            List<?> rawResult = fetcher.fetchData(currentIin, effectiveDateFrom, effectiveDateTo);

                            if (rawResult.isEmpty()) continue;

                            List<?> filteredResult = fetcher.filterRecords(rawResult, currentIin, mainIin, isUl);

                            if (filteredResult.isEmpty()) continue;

                            Object first = filteredResult.get(0);
                            if (first instanceof ESFInformationRecordDt) {
                                synchronized (esfRecords) {
                                    int before = esfRecords.size();
                                    int added = 0;

                                    for (Object obj : filteredResult) {
                                        ESFInformationRecordDt record = (ESFInformationRecordDt) obj;
                                        String key = RecordKeyGenerator.generateKey(record);

                                        if (esfKeys.add(key)) {
                                            esfRecords.add(record);
                                            added++;
                                        }
                                    }

                                    log.info("ESF list: {} → {} (added {} out of {} from {})",
                                            before, esfRecords.size(), added, filteredResult.size(),
                                            meta.getSources());
                                }
                            } else if (first instanceof InformationRecordDt) {
                                synchronized (infoRecords) {
                                    int added = 0;

                                    for (Object obj : filteredResult) {
                                        InformationRecordDt record = (InformationRecordDt) obj;
                                        String key = RecordKeyGenerator.generateKey(record);

                                        if (infoKeys.add(key)) {
                                            infoRecords.add(record);
                                            added++;
                                        }
                                    }

                                    log.debug("Info records added: {} out of {} from {}",
                                            added, filteredResult.size(), meta.getSources());
                                }
                            } else if (first instanceof NaoConRecordDt) {
                                synchronized (naoConRecordDs) {
                                    int added = 0;

                                    for (Object obj : filteredResult) {
                                        NaoConRecordDt record = (NaoConRecordDt) obj;
                                        String key = RecordKeyGenerator.generateKey(record);

                                        if (naoConKeys.add(key)) {
                                            naoConRecordDs.add(record);
                                            added++;
                                        }
                                    }

                                    log.debug("NaoCon records added: {} out of {} from {}",
                                            added, filteredResult.size(), meta.getSources());
                                }
                            }

                        } catch (Exception e) {
                            log.error("Failed to fetch data from {} for IIN {}: {}",
                                    source, currentIin, e.getMessage(), e);
                        }
                    }
                }
            }
        });
    }

    private List<RecordDt> deduplicateRecords(List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> seenKeys = new HashSet<>();
        List<RecordDt> deduplicated = new ArrayList<>();

        for (RecordDt record : records) {
            String key;

            if (record instanceof ESFInformationRecordDt esf) {
                key = RecordKeyGenerator.generateKey(esf);
            } else if (record instanceof InformationRecordDt info) {
                key = RecordKeyGenerator.generateKey(info);
            } else if (record instanceof NaoConRecordDt nao) {
                key = RecordKeyGenerator.generateKey(nao);
            } else {
                key = record.toString();
                log.warn("Unknown record type for deduplication: {}", record.getClass().getName());
            }

            if (seenKeys.add(key)) {
                deduplicated.add(record);
            }
        }

        log.info("Deduplicated records: {} → {} (removed {})",
                records.size(), deduplicated.size(), records.size() - deduplicated.size());

        return deduplicated;
    }

    private List<YearlyCount> calculateYearlyCounts(List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        return records.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getDate() != null)
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getDate().getYear()),
                        TreeMap::new,
                        Collector.of(
                                () -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO },
                                (acc, r) -> {
                                    acc[0] = acc[0].add(BigDecimal.ONE);
                                    BigDecimal val = numberConverter.parseBigDecimalOrZero(r.getSumm());
                                    acc[1] = acc[1].add(val);
                                },
                                (acc1, acc2) -> {
                                    acc1[0] = acc1[0].add(acc2[0]);
                                    acc1[1] = acc1[1].add(acc2[1]);
                                    return acc1;
                                },
                                acc -> YearlyCount.builder()
                                        .count(acc[0].intValueExact())
                                        .summ(acc[1].toPlainString())
                                        .build()
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    YearlyCount base = entry.getValue();
                    return YearlyCount.builder()
                            .year(entry.getKey())
                            .count(base.getCount())
                            .summ(base.getSumm())
                            .build();
                })
                .toList();
    }}
