package org.info.infobaza.model.info.job;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TurnoverRecord {
    private int amount;
    private LocalDateTime date;
    private String iin;
    private String bank;
    private String account;
    private String dopinfo;
}
