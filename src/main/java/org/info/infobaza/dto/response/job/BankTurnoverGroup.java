package org.info.infobaza.dto.response.job;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.model.info.job.TurnoverRecord;

import java.util.List;

@Getter
@Setter
@Builder
public class BankTurnoverGroup {
    private String bankName;
    private Integer count;
    private List<TurnoverRecord> records;
}