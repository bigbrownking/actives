package org.info.infobaza.dto.response.info;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RecordGroup {
    private String year;
    private String totalSum;
    private String oper;
    private String iinsInvolved;
    private String info;
}
