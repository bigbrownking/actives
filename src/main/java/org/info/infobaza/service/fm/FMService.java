package org.info.infobaza.service.fm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class FMService implements AbstractService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FM-1", "FM"},
            vids = {"Денежные средства"},
            isActive = true
    )
    public List<InformationRecordDt> getFMMoney(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching fm money for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FM_Денежные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    @ServiceMetadata(
            type = {"Реализация", "Приобретение"},
            source = {"FM-1", "FM"},
            vids = {"Ценные бумаги"},
            isActive = true
    )
    public List<InformationRecordDt> getFMPaper(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching fm paper for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FM_Ценные_бумаги.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    @ServiceMetadata(
            type = {"Приобретение"},
            source = {"FM-1", "FM"},
            vids = {"Цифровые активы"},
            isActive = true
    )
    public List<InformationRecordDt> getFMActiv(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching fm activ for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FM_Цифровые_активы.getPath(), iin, dateFrom ,dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FM-1", "FM"},
            vids = {"Сейфовые ячейки"},
            isActive = true
    )
    public List<InformationRecordDt> getFMSafe(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching fm safe for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FM_Сейфовые_ячейки.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    @ServiceMetadata(
            type = {"Реализация", "Приобретение"},
            source = {"FM-1", "FM"},
            vids = {"Недвижимое имущество"},
            isActive = true
    )
    public List<InformationRecordDt> getFMHouse(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching fm house for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FM_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }
}
