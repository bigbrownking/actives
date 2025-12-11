package org.info.infobaza.dto.response.relation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.info.infobaza.model.info.job.SupervisorRecord;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@ToString
public class RelationActive {
    private String relation;
    private String fio;
    private String iin;
    private String actives;
    private String incomes;
    private Map<String, String> dopinfo;
    private List<SupervisorRecord> head;
    private String info;
    private int level;
}
