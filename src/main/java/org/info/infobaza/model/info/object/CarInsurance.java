package org.info.infobaza.model.info.object;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
public class CarInsurance {
    private String marka;
    private String model;
    private String vin;
    private String grnz;
    private String iinInsurer;
    private String iinInsured;
    private LocalDate startDate;
    private LocalDate endDate;
}
