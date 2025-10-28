package org.info.infobaza.dto.request;

import lombok.Getter;
import org.info.infobaza.util.logging.Loggable;

import java.util.List;

@Getter
public class MassExportRequest {
    @Loggable
    private List<String> iins;
}
