package org.info.infobaza.model.info.person.nominal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NominalFiz implements Nominal{
    private String iin_bin;
    private int first_asset_year;
    private long total_sum;
    private double avg_month_income;
    private double ratio;
}
