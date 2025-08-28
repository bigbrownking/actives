package org.info.infobaza.service.esf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
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
public class ESFService implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            type = {"Реализация", "Приобретение"},
            source = {"ESF"},
            vids = {"Транспортные средства"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFTransport(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf transport for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Транспортные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"ESF"},
            vids = {"Недвижимое имущество"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFHouse(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf transport for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"ESF"},
            vids = {"Украшения и золото"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFJewelry(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf jewelry for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Украшения_и_золото.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение"},
            source = {"ESF"},
            vids = {"Прочие активы"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFOther(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf gold for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Прочие_активы.getPath(), iin, dateFrom, dateTo );
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение"},
            source = {"ESF"},
            vids = {"Турпакеты", "Прочие активы"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFTur(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf tur for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Турпакеты.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Реализация", "Приобретение"},
            source = {"ESF"},
            vids = {"Животные"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFAnimals(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf animals for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Животные.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"ESF"},
            vids = {"Предметы исскуства"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFPicture(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf picture for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Предметы_исскуства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"ESF"},
            vids = {"Сейфовые ячейки"},
            isActive = true
    )
    public List<ESFInformationRecordDt> getESFSafe(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching esf safe for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ESF_Сейфовые_ячейки.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToESF);
    }

}
