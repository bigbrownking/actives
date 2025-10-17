package org.info.infobaza.dto.response.info.active;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.dto.response.info.RecordGroup;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class OverallActive implements ActiveResponse{
    private String dateFrom;
    private String dateTo;
    private List<String> selectedYears;
    private List<RecordGroup> recordsByOper;
    private Map<String, List<String>> iinToRelation;
}