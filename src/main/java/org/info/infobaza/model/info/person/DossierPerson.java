package org.info.infobaza.model.info.person;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.info.infobaza.dto.response.person.Age;

@Data
@AllArgsConstructor
public class DossierPerson {
    private String firstName;
    private String lastName;
    private String patronymic;
    private String birthDateStr;
    private String deathDateStr;
    private Age age;
    private String photo;
}
