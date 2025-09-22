package org.info.infobaza.model.info.job;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TurnoverRecord {
    private String iinBin;
    private String bankName;
    private String bankAccount;
    private String summ;
    private String startDate;
    private String endDate;
    private String source;

}
