package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class NaoConDataFetcher implements DataFetcher {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;
    private final FetcherConfig config;

    @Override
    public List<NaoConRecordDt> fetchData(String iin, String dateFrom, String dateTo) {
        // Similar to ESF but uses appropriate mapper
        try {
            String sql = sqlFileUtil.getSqlWithIinAndDates(
                    config.getQueryPath().getPath(), iin, dateFrom, dateTo
            );
            return jdbcTemplate.query(sql, mapper::mapRowToNaoCon); // Adjust mapper method
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch NaoCon data", e);
        }
    }

    @Override
    public List<NaoConRecordDt> filterRecords(List<?> records, String currentIin, String mainIin, boolean isUl) {
        if (records.isEmpty()) return List.of();

        List<NaoConRecordDt> naoConRecords = records.stream()
                .map(NaoConRecordDt.class::cast)
                .toList();

        if (!isUl) {
            // No filtering for non-UL
            return naoConRecords;
        }

        List<NaoConRecordDt> filtered = naoConRecords.stream()
                .filter(record ->
                        (currentIin.equals(record.getIin_bin_pokup()) && mainIin.equals(record.getIin_bin_prod())) ||
                                (currentIin.equals(record.getIin_bin_prod()) && mainIin.equals(record.getIin_bin_pokup())))
                .toList();

        log.debug("NaoCon records for iin={}, isUl={}: {} records before filter, {} after",
                currentIin, isUl, naoConRecords.size(), filtered.size());

        return filtered;
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