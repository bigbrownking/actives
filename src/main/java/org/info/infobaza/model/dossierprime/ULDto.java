package org.info.infobaza.model.dossierprime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ULDto {
    private String bin;
    private String fullName;
    private String oked;
    private String status;
    private String regDate;
    private Double infoPercentage;
    private Double riskPercentage;
    private Boolean isResident;
}