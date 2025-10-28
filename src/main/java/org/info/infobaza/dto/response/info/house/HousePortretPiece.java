package org.info.infobaza.dto.response.info.house;

import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.dto.response.person.Person;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class HousePortretPiece {
    private String fio;
    private int age;
    private String iin;
    private String image;
    private List<String> portret;
    private Boolean isCryptoActive;
    private Boolean isNominal;
    private Boolean isNominalUl;
    private String iinRukUch;

    private LocalDate date;
    private boolean buy;

    public void copy(Person person){
        this.fio = person.getFio();
        this.age = person.getAge();
        this.iin = person.getIin();
        this.image = person.getImage();
        this.portret = person.getPortret();
        this.isCryptoActive = person.getIsCryptoActive();
        this.isNominal = person.getIsNominal();
        this.isNominalUl = person.getIsNominalUl();
        this.iinRukUch = person.getIinRukUch();
    }
}
