package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.job.Industry;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndustrialService {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;

    public Industry getIndustry(String iin) throws IOException {
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Industry_industry.getPath(), iin);
        try {
            return jdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> new Industry(rs.getString("naimenovanie")));
        }catch (EmptyResultDataAccessException e){
            log.warn("No industry found for IIN: {}", iin);
            return null;
        }
    }
}
