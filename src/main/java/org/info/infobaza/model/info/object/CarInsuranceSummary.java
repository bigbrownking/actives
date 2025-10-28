package org.info.infobaza.model.info.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarInsuranceSummary {
    private String marka;
    private String model;
    private String vin;
    private String grnz;
    private String iinInsurer;
    private List<String> iinInsured;
    private LocalDate startDate;
    private LocalDate endDate;
}
