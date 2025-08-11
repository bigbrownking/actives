package org.info.infobaza.model.info;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NominalFiz {
    private String iin_bin;
    private int first_asset_year;
    private long total_sum;
    private double avg_month_income;
    private double ratio;
}
