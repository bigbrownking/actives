package org.info.infobaza.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.info.infobaza.util.logging.Loggable;

import java.time.LocalDate;
import java.util.Objects;

@Getter
public class MainRequest {
    @NotBlank(message = "IIN is required and cannot be blank")
    @Loggable
    private String iin_bin;

    @NotNull(message = "Start date is required")
    @Loggable
    private LocalDate dateFrom;

    @NotNull(message = "End date is required")
    @Loggable
    private LocalDate dateTo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MainRequest that = (MainRequest) o;
        return Objects.equals(iin_bin, that.iin_bin) &&
                Objects.equals(dateFrom, that.dateFrom) &&
                Objects.equals(dateTo, that.dateTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iin_bin, dateFrom, dateTo);
    }
}
