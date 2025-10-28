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
    public List<NaoConRecordDt> searchNaoByAddress(String city,
                                          String district,
                                          String street,
                                          String house,
                                          String apartment) {
        StringBuilder query = new StringBuilder("""
        SELECT *
        FROM pfr_dashboard.active_table_2_1_tb
        WHERE 1=1
        """);
        if (city != null && !city.isBlank())
            query.append(" AND adress ILIKE '%").append("г. ").append(city.trim().replace("'", "''")).append("%'");
        if (district != null && !district.isBlank())
            query.append(" AND adress ILIKE '%").append("р-н ").append(district.trim().replace("'", "''")).append("%'");
        if (street != null && !street.isBlank())
            query.append(" AND adress ILIKE '%").append(street.trim().replace("'", "''")).append("%'");
        if (house != null && !house.isBlank())
            query.append(" AND adress ILIKE '%").append("д. ").append(house.trim().replace("'", "''")).append("%'");
        if (apartment != null && !apartment.isBlank())
            query.append(" AND adress ILIKE '%").append("кв. ").append(apartment.trim().replace("'", "''")).append("%'");

        log.info("Final SQL query: {}", query);

         List<NaoConRecordDt> recordDts = jdbcTemplate.query(query.toString(), mapper::mapRowToNaoCon);
         log.info("Records size :{} ", recordDts.size());
        return recordDts;
    }
}
