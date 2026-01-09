package org.info.infobaza.dto.response.info.yearlyCounts;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class SingleIinYearlyCounts {
    private final List<YearlyCount> counts;
}
