package org.info.infobaza.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.info.infobaza.util.logging.Loggable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YearlyCountRequest {
    @NotBlank(message = "IIN is required and cannot be blank")
    @Loggable
    private String iin;

    @NotBlank(message = "dateFrom is required and cannot be blank")
    @Loggable
    private LocalDate dateFrom;

    @NotBlank(message = "dateTo is required and cannot be blank")
    @Loggable
    private LocalDate dateTo;

    @Nullable
    @Loggable
    private List<String> sources;

    @Nullable
    @Loggable
    private List<String> types;

    @Nullable
    @Loggable
    private List<String> iins;

    @Loggable
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YearlyCountRequest that = (YearlyCountRequest) o;
        return active == that.active &&
                Objects.equals(iin, that.iin) &&
                Objects.equals(dateFrom, that.dateFrom) &&
                Objects.equals(dateTo, that.dateTo) &&
                Objects.equals(sources, that.sources) &&
                Objects.equals(types, that.types) &&
                Objects.equals(iins, that.iins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iin, dateFrom, dateTo, sources, types, iins, active);
    }
}
