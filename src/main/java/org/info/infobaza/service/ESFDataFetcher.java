package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ESFDataFetcher implements DataFetcher {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;
    private final FetcherConfig config;

    @Override
    public List<ESFInformationRecordDt> fetchData(String iin, String dateFrom, String dateTo) {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        log.info("Fetching ESF {} for IIN: {}", config.getVids()[0], iin);

        try {
            String sql = sqlFileUtil.getSqlWithIinAndDates(
                    config.getQueryPath().getPath(), iin, dateFrom, dateTo
            );
            return jdbcTemplate.query(sql, mapper::mapRowToESF);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch ESF data", e);
        }
    }

    @Override
    public List<ESFInformationRecordDt> filterRecords(List<?> records, String currentIin, String mainIin, boolean isUl) {
        if (records.isEmpty()) return List.of();

        List<ESFInformationRecordDt> esfRecords = records.stream()
                .map(ESFInformationRecordDt.class::cast)
                .toList();

        // Apply the same filtering logic from your original code
        if (!isUl || mainIin.equals(currentIin)) {
            log.info("ESF: iin={} (isUl={}, main==current={}) → TAKING ALL {} records",
                    currentIin, isUl, mainIin.equals(currentIin), esfRecords.size());
            return esfRecords;
        } else {
            List<ESFInformationRecordDt> filtered = esfRecords.stream()
                    .filter(record ->
                            (currentIin.equals(record.getIin_bin_pokup()) && mainIin.equals(record.getIin_bin_prod())) ||
                                    (currentIin.equals(record.getIin_bin_prod()) && mainIin.equals(record.getIin_bin_pokup())))
                    .toList();
            log.info("ESF: iin={} (isUl={}, main!=current) → filtered: {} → {}",
                    currentIin, isUl, esfRecords.size(), filtered.size());
            return filtered;
        }
    }

    @Override
    public FetcherMetadata getMetadata() {
        return new FetcherMetadata(
                config.getTypes(),
                new String[]{config.getSource()},
                config.getVids(),
                config.isActive(),
                config.isIncome()
        );
    }
}
