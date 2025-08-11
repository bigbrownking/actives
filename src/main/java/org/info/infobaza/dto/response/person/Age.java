package org.info.infobaza.dto.response.person;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Age {
    private int age;
    private boolean status;
}
