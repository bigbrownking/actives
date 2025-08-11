package org.info.infobaza.dto.response.info;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ServiceSources {
    private List<String> incomeSources;
    private List<String> activeSources;
    private List<String> activeTypes;
    private List<String> activeVids;
    private List<String> incomeVids;
}