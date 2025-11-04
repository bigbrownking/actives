package org.info.infobaza.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.model.info.active_income.EsfOverall;
import org.info.infobaza.model.info.job.SupervisorRecord;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Head {
    private List<SupervisorRecord> head; // уч или рук
    private Double income; // доход
    private Double tax; // уплата налогов
    private List<EsfOverall> esf; // esf
    private List<String> statuses; // status
    public boolean isEmpty(){
        return head.isEmpty() && esf.isEmpty() && statuses.isEmpty();
    }
}
