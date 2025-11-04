package org.info.infobaza.dto.request;

import jakarta.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.info.infobaza.util.logging.LogRequest;
import org.info.infobaza.util.logging.Loggable;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class FindHouseRequest {
    @Nullable
    @Loggable
    private String kd;

    @Nullable
    @Loggable
    private String rka;

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
