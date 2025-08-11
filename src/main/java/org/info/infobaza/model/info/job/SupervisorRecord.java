package org.info.infobaza.model.info.job;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupervisorRecord {
    private String iin_bin;
    private String positionType;
    private String taxpayer_iin_bin;
    private String taxpayerType;
    private String taxpayerName;
}
