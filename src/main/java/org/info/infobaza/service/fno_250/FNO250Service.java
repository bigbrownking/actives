package org.info.infobaza.service.fno_250;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.service.AbstractService;
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
public class FNO250Service implements AbstractService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO250"},
            vids = {"Иные имущества"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO250Other(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO250 other for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO250_Иные_имущества.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO250"},
            vids = {"Прочие активы"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getFNO250Other2(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO250 money for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO250_Прочие_активы.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }

    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO250"},
            vids = {"Недвижимое имущество"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO250House(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO250 house for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO250_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO250"},
            vids = {"Ценные бумаги"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getFNO250Paper(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO250 paper for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO250_Ценные_бумаги.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }

    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO250"},
            vids = {"Транспортные средства"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO250Transport(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO250 transport for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO250_Транспортные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }
}
