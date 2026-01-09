package org.info.infobaza.service.nao_con;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NaoConService implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @Cacheable(
            value = "naoConHouse",
            key = "#iin + '_' + #dateFrom + '_' + #dateTo"
    )
    @ServiceMetadata(
            source = {"НАО ЦОН"},
            vids = {"Недвижимое имущество"},
            type = {"Реализация", "Приобретение"},
            isActive = true
    )
    public List<NaoConRecordDt> getNaoConHouse(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching nao con house for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.НАО_ЦОН_Недвижимое_имущество.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToNaoCon);
    }

    public List<NaoConRecordDt> searchNaoByKdRka(String kd, String rka) throws IOException {
        if (kd != null && !kd.isBlank()) {
            String sql = sqlFileUtil.getSqlWithIin(
                    QueryLocationDictionary.НАО_ЦОН_Недвижимое_имущество_kd.getPath(), kd);
            log.info("kd provided: {}", kd);
            return jdbcTemplate.query(sql, mapper::mapRowToNaoCon);
        }

        if (rka != null && !rka.isBlank()) {
            String sql = sqlFileUtil.getSqlWithIin(
                    QueryLocationDictionary.НАО_ЦОН_Недвижимое_имущество_rka.getPath(), rka);
            log.info("rka provided: {}", rka);
            return jdbcTemplate.query(sql, mapper::mapRowToNaoCon);
        }
        return null;
    }
}
