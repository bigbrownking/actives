package org.info.infobaza.service.enpf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.job.Industry;
import org.info.infobaza.exception.NotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndustrialService {
    private final JdbcTemplate jdbcTemplate;

    public Industry getIndustry(String iin) throws NotFoundException {
        String sql = "SELECT * FROM pfr_dashboard.ip_kh WHERE iin_bin = ?";
        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    new Object[]{iin},
                    (rs, rowNum) -> new Industry(rs.getString("naimenovanie"))
            );
        }catch (EmptyResultDataAccessException e){
            log.warn("No industry found for IIN: {}", iin);
            throw new NotFoundException("No industry found");
        }
    }
}
