package org.info.infobaza.dto.response.info.income;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.model.info.active_income.RecordDt;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class IncomeWithRecords implements IncomeResponse{
    private String dateFrom;
    private String dateTo;
    private List<String> selectedYears;
    private List<RecordDt> recordsByYear;
    private Map<String, List<String>> iinToRelation;
}