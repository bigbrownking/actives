package org.info.infobaza.dto.response.info.yearlyCounts;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class YearlyRecordCounts {
    private List<YearlyCount> counts;
}
