package org.info.infobaza.dto.request;

import lombok.Getter;
import org.info.infobaza.util.logging.Loggable;

@Getter
public class GetPortretRequest {
    @Loggable
    private String iin;
}
