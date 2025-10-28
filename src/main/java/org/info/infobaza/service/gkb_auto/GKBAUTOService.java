package org.info.infobaza.service.gkb_auto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
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
public class GKBAUTOService implements InformationalService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;


    @ServiceMetadata(type = {"Наличие"}, source = {"ГКБ Cведения по страховке Авто", "Cведения по страховке Авто"}, vids = {"Транспортные средства", "ГКБ-Транспортные средства"}, isActive = true)
    public List<InformationRecordDt> getGKBAuto(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching gkb auto for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIinAndDates(QueryLocationDictionary.ГКБ_Cведения_по_страховке_Авто_Транспортные_средства.getPath(), iin, dateFrom, dateTo);
        return jdbcTemplate.query(sql, mapper::mapRowToInformation);
    }

    public List<InformationRecordDt> searchCarByParams(String vin, String grnz, String mark) {
        StringBuilder query = new StringBuilder("""
                	SELECT *
                	FROM (
                	select
                	    iin_zastrahovanny as iin_bin,
                	    min(toDate(start_date)) as date,
                	    concat('ГКБ-Cведения по страховке Авто') as database,
                	    concat('Транспортные средства') as aktivy,
                	    concat('Наличие') as oper,
                	    concat('Авто:',coalesce(marka, '0'),' ',model,'; VIN код:',vin_kod, '; ГРНЗ:', grnz, '; ИИН страхователя:',coalesce(iin_strahovatel, '0'),';') as dopinfo,
                	    toInt32(concat('0')) as summ
                	from pfr_dashboard.auto_vin
                	group by iin_bin, database, aktivy, oper, summ, marka, model, iin_strahovatel, vin_kod, grnz
                	
                	union all
                	
                	
                	
                	select
                	    iin_strahovatel as iin_bin,
                	    min(toDate(start_date)) as date,
                	    concat('Cведения по страховке Авто') as database,
                	    concat('ГКБ-Транспортные средства') as aktivy,
                	    concat('Наличие') as oper,
                	    concat('Авто:',coalesce(marka, '0'),' ',model,'; VIN код:',vin_kod, '; ГРНЗ:', grnz, '; ИИН застрахованного:',coalesce(iin_zastrahovanny, '0'),';') as dopinfo,
                	    toInt32(concat('0')) as summ
                	from pfr_dashboard.auto_vin where iin_zastrahovanny !='0'
                	group by iin_bin, database, aktivy, oper, summ, marka, model, iin_zastrahovanny, vin_kod, grnz
                	) AS subquery
                	WHERE 1=1
                        """);
        if (vin != null && !vin.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(vin.trim().replace("'", "''")).append("%'");
        if (grnz != null && !grnz.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(grnz.trim().replace("'", "''")).append("%'");
        if (mark != null && !mark.isBlank())
            query.append(" AND dopinfo ILIKE '%").append(mark.trim().replace("'", "''")).append("%'");

        log.info("Final SQL query: {}", query);

        List<InformationRecordDt> recordDts = jdbcTemplate.query(query.toString(), mapper::mapRowToInformation);
        log.info("Records size :{} ", recordDts.size());
        return recordDts;
    }
}
