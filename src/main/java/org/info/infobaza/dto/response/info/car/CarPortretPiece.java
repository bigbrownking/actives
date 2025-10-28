package org.info.infobaza.dto.response.info.car;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.info.infobaza.dto.response.person.Person;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CarPortretPiece {
    private String fio;
    private int age;
    private String iin;
    private String image;
    private List<String> portret;
    private Boolean isCryptoActive;
    private Boolean isNominal;
    private Boolean isNominalUl;
    private String iinRukUch;

    private LocalDate startDate;
    private LocalDate endDate;    private String role;


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
    public void mergeDates(LocalDate start, LocalDate end) {
        if (start != null) {
            if (this.startDate == null || start.isBefore(this.startDate)) {
                this.startDate = start;
            }
        }
        if (end != null) {
            if (this.endDate == null || end.isAfter(this.endDate)) {
                this.endDate = end;
            }
        }
    }

}
