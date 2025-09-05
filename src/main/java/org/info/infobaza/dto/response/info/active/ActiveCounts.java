package org.info.infobaza.dto.response.info.active;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ActiveCounts {
    private String iin_bin;
    private LocalDate date;
    private String oper;
    private String dopinfo;
    private String num_doc;
    private String summ;
}
