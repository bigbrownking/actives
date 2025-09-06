package org.info.infobaza.dto.response.job;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Turnover {
    private String bank;
    private String account;
    private long summ;
    private long positiveSumm;
    private long negativeSumm;
    private int count;
}
