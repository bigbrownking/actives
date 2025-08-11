package org.info.infobaza.service.fno_270;

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
public class FNO270Service implements AbstractService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;

    @ServiceMetadata(
            type = {"Приобретение", "Реализация"},
            source = {"FNO270"},
            vids = {"Недвижимое имущество"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO270House(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 house for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO270"},
            vids = {"Иные имущества"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO270Other(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 other for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_Иные_имущества.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }


    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"Доход от осуществления нотариуса, судебного исполнителя, адвоката, профессионального медиатора"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270IncomeNotarius(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income notarius for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_Доход_от_осуществления_нотариуса_судебного_исполнителя_адвоката_профессионального_медиатора.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"в. т. ч. Доход от ИП"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270IncomeIP(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income ip for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_в_т_ч_Доход_от_ИП.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"в. т. ч. Доход из источников за пределами РК"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270IncomeAbroad(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income abroad for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_в_т_ч_Доход_из_источников_за_пределами_РК.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"в.т.ч. Доход лица занимающиеся частной практикой"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270IncomePractice(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income practice for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_в_т_ч_Доход_лица_занимающиеся_частной_практикой.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"в.т.ч. Имущественный доход"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270IncomeItems(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income items for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_в_т_ч_Имущественный_доход.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }


    @ServiceMetadata(
            source = {"FNO270"},
            vids = {"Доход по данным ФНО"},
            isIncome = true
    )
    public List<InformationRecordDt> getFNO270Income(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_Доход_по_данным_ФНО.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"FNO270"},
            vids = {"Прочие активы"},
            isActive = true
    )
    public List<InformationRecordDt> getFNO270Other2(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching FNO270 income for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.FNO270_Прочие_активы.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToIncome);
    }

}
