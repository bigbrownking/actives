package org.info.infobaza.dto.response.person;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Person {
    private String fio;
    private int age;
    private String iin;
    private String image;
    private List<String> portret;
    private Boolean isCryptoActive;
    private String actives;
    private String incomes;
    private Boolean isNominal;
    private Boolean isNominalUl;
}
