package org.info.infobaza.dto.response.info;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.model.info.active_income.InformationRecordDt;

import java.util.List;

@Getter
@Setter
@Builder
public class RecordsWithTotal {
    private List<InformationRecordDt> records;
    private double totalSum;
}
