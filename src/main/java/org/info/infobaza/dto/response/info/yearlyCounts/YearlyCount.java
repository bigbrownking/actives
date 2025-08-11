package org.info.infobaza.dto.response.info.yearlyCounts;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YearlyCount {
    private String year;
    private Integer count;
    private String summ;
}
