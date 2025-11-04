package org.info.infobaza.dto.response.info.car;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CarInfo {
    private String mark;
    private String model;
    private String vin;
    private String grnz;
}
