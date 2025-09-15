package org.info.infobaza.dto.response.info;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IinInfo {
    private String type;
    private String name;
    private String iin;
}