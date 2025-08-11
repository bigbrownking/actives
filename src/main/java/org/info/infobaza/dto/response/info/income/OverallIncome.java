package org.info.infobaza.dto.response.info.income;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.dto.response.info.RecordGroup;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class OverallIncome implements IncomeResponse{
    private String dateFrom;
    private String dateTo;
    private List<String> selectedYears;
    private List<RecordGroup> recordsByYear;
    private Map<String, List<String>> iinToRelation;
}
