package org.info.infobaza.service.kap_mvd_auto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
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
public class KAPMVDAUTOService implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(
            type = {"Наличие"},
            source = {"КАП МВД Cведения по владельцам Авто"},
            vids = {"Транспортные средства"},
            isActive = true
    )
    public List<InformationRecordDt> getKapMvdAuto(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching kap mvd auto for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.КАП_МВД_Cведения_по_владельцам_Авто_Транспортные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    public List<InformationRecordDt> searchCarByParams(String vin, String grnz, String mark){
        StringBuilder query = new StringBuilder("""
                select
                    iin_owners as iin_bin,
                    date_of_registration as date,
                    concat('КАП МВД-Cведения по владельцам Авто') as database,
                    concat('Транспортные средства') as aktivy,
                    concat('Наличие') as oper,
                    concat('Авто:',marka,'; Год авто:',year_of_release,'; VIN код:',vin_kod,'; ГРНЗ:',registration_number,';') as dopinfo,
                    toInt32(concat('0')) as summ
                from pfr_dashboard.auto_VIN_vladelcy
                WHERE 1=1
        """);
        if (vin != null && !vin.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(vin.trim().replace("'", "''")).append("%'");
        if (grnz != null && !grnz.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(grnz.trim().replace("'", "''")).append("%'");
        if (mark != null && !mark.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(mark.trim().replace("'", "''")).append("%'");

        log.info("Final SQL query: {}", query);

        return jdbcTemplate.query(query.toString(), mapper::mapRowToInformation);
    }
}
