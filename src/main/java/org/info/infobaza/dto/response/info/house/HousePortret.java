package org.info.infobaza.dto.response.info.house;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.info.infobaza.dto.response.person.Person;

import java.util.List;

@Data
@Builder
public class HousePortret {
    private String kd;
    private String rka;
    private String currentOwner;
    private String volume;
    private List<HousePortretPiece> portrets;
}
