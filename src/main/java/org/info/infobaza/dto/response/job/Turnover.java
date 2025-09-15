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
}
