package org.info.infobaza.model.info.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@ToString
public class Car {
    private String mark;
    private String model;
    private String vin;
    private String grnz;
    private int yearRelease;
    private int capacity;
    private String iinOwner;
    private LocalDate dateRegistration;
    private String clazz;
}
