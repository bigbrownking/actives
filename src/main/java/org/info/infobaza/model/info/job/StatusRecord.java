package org.info.infobaza.model.info.job;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class StatusRecord {
    private String iin_bin;
    private String status;
}
