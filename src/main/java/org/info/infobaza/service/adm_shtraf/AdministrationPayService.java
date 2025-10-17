package org.info.infobaza.service.adm_shtraf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.job.PenaltyRecord;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministrationPayService implements InformationalService {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;

    public List<PenaltyRecord> getAdministrationPay(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching administration pay for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.Административные_штрафы.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToPenalty);
    }

//    @ServiceMetadata(
//            type = {"Приобретение", "Реализация"},
//            source = {"КСК"},
//            vids = {"Коммунальные платежи за третих лиц"},
//            isActive = true
//    )
    public List<ESFInformationRecordDt> getPublicUtilities(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching administration pay for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.Коммунальные.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToUtil);
    }

}
