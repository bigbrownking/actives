package org.info.infobaza.dto.response.info.active;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class SingleIinActiveResult {
    private final List<InformationRecordDt> infoRecords;
    private final List<ESFInformationRecordDt> esfRecords;
    private final List<NaoConRecordDt> naoRecords;
}

