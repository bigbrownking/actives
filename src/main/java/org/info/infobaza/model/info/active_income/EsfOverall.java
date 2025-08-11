package org.info.infobaza.model.info.active_income;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class EsfOverall {
    private String iin_bin;
    private LocalDate date;
    private String aktivy;
    private Long summ;
}
