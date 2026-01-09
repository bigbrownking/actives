package org.info.infobaza.dto.response.info.income;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.info.infobaza.model.info.active_income.InformationRecordDt;

import java.util.List;

@Getter
@AllArgsConstructor
public class SingleIinIncomeResult {
    private final List<InformationRecordDt> infoRecords;
}