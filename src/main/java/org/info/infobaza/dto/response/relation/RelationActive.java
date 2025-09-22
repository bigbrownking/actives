package org.info.infobaza.dto.response.relation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.info.infobaza.model.info.job.SupervisorRecord;

import java.util.List;

@Getter
@Builder
@ToString
public class RelationActive {
    private String relation;
    private String fio;
    private String iin;
    private String actives;
    private String incomes;
    private List<SupervisorRecord> head;
    private boolean isNominal;
    private int level;
}
