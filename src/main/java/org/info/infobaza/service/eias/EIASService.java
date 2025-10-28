package org.info.infobaza.service.eias;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EIASService implements InformationalService {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"ЕИАС"},
            vids = {"Недвижимое имущество"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getEiasHouse(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching eias house for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕИАС_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение"},
            source = {"ЕИАС"},
            vids = {"Транспортные средства"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getEiasTransport(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching eias transport for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕИАС_Транспортные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Реализация"},
            source = {"ЕИАС"},
            vids = {"Украшения и золото"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getEiasJewelry(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching eias jewelry for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕИАС_Украшения_и_золото.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }

    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"ЕИАС"},
            vids = {"Ценные бумаги"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getEiasPaper(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching eias paper for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕИАС_Ценные_бумаги.getPath(), iin, dateFrom, dateTo);
        List<ESFInformationRecordDt> esfInformationRecordDts =  jdbcTemplate.query(sql, mapper::mapRowToESF);
        log.info("SIZEIS : "+ esfInformationRecordDts.size());
        return esfInformationRecordDts;
    }


    @ServiceMetadata(
            type = {"Приобретение", "Реализация", "Наличие"},
            source = {"ЕИАС"},
            vids = {"Цифровые активы"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getEiasActives(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching eias actives for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ЕИАС_Цифровые_активы.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }
}
