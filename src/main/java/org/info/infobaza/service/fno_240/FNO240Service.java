package org.info.infobaza.service.fno_240;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FNO240Service implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;

    @ServiceMetadata(
            source = {"FNO240"},
            vids = {"Прибыль КИК"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO240IncomeKik(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO240 income kik for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO240_Прибыль_КИК.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO240"},
            vids = {"Иные имущества"},
            isActive = true
    )

    public List<InformationRecordDt> getFNO240Other(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO240 other for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO240_Иные_имущества.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }


}
