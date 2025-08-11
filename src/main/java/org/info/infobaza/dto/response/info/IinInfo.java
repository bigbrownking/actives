package org.info.infobaza.dto.response.info;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IinInfo {
    private final String type;
    private final String name;
    private final String iin;
}