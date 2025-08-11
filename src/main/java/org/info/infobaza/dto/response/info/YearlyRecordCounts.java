package org.info.infobaza.dto.response.info;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class YearlyRecordCounts {
    private List<YearlyCount> counts;
}
