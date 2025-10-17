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
}
