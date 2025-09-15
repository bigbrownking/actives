package org.info.infobaza.dto.response.info;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.model.info.active_income.RecordDt;

import java.util.List;

@Getter
@Setter
@Builder
public class ActiveCountGroup {
    private String database;
    private String aktivy;
    private String type;
    private Integer count;
    private List<RecordDt> records;
}