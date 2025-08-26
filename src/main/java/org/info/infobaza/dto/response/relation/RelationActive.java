package org.info.infobaza.dto.response.relation;

import lombok.Builder;
import lombok.Getter;
import org.info.infobaza.dto.response.job.Head;

@Getter
@Builder
public class RelationActive {
    private String relation;
    private String fio;
    private String iin;
    private String actives;
    private String incomes;
    private Head head;
    private boolean isNominal;
    private int level;
}
