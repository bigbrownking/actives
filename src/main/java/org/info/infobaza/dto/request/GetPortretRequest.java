package org.info.infobaza.dto.request;

import jakarta.annotation.Nullable;
import lombok.Getter;
import org.info.infobaza.util.logging.Loggable;

@Getter
public class GetPortretRequest {
    @Loggable
    private String iin;

    @Loggable
    private String dateFrom;

    @Loggable
    private String dateTo;

    @Nullable
    @Loggable
    private String orderNum;

    @Nullable
    @Loggable
    private String approvement_type;

    @Nullable
    @Loggable
    private String caseNum;

    @Nullable
    @Loggable
    private String orderDate;

    @Nullable
    @Loggable
    private String articleName;

    @Nullable
    @Loggable
    private String checkingName;

    @Nullable
    @Loggable
    private String otherReasons;

    @Nullable
    @Loggable
    private String organName;

    @Nullable
    @Loggable
    private String sphereName;

    @Nullable
    @Loggable
    private String tematikName;

    @Nullable
    @Loggable
    private String rukName;
}
