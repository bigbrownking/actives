package org.info.infobaza.dto.response.info.house;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@EqualsAndHashCode
public class HouseHistory {
    private String seller;
    private String buyer;
    private LocalDate date;
    private String summ;
}
