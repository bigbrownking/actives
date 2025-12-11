package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SimpleDataFetcher implements DataFetcher {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;
    private final FetcherConfig config;

    @Override
    public List<InformationRecordDt> fetchData(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        log.info("Fetching {} {} for IIN: {}", config.getSource(), config.getVids()[0], iin);

        String sql = sqlFileUtil.getSqlWithIinAndDates(
                config.getQueryPath().getPath(), iin, dateFrom, dateTo
        );

        try {
            return jdbcTemplate.query(sql, mapper::mapRowToInformation);
        } catch (Exception mappingError) {
            log.warn("Information mapping failed, fallback to Income mapper. Cause: {}", mappingError.getMessage());
            return jdbcTemplate.query(sql, mapper::mapRowToIncome);
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
