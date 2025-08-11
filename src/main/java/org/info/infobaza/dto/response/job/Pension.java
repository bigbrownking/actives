package org.info.infobaza.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class Pension {
    private String dateFrom;
    private String dateTo;
    private String name;
    private String P_RNN;
}
