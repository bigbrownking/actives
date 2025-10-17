package org.info.infobaza.dto.response.person;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Person {
    private String fio;
    private int age;
    private String iin;
    private String image;
    private List<String> portret;
    private Boolean isCryptoActive;
    private Boolean isNominal;
}
