package org.info.infobaza.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import org.info.infobaza.model.main.Request;
import org.info.infobaza.util.logging.Loggable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
public class RelativesActiveRequest extends Request {

    @NotBlank(message = "IIN is required and cannot be blank")
    @Loggable
    private String iin;

    @NotBlank(message = "dateFrom is required and cannot be blank")
    @Loggable
    private LocalDate dateFrom;

    @NotBlank(message = "dateTo is required and cannot be blank")
    @Loggable
    private LocalDate dateTo;

    @NotBlank
    @Loggable
    private List<String> years;

    @Nullable
    @Loggable
    private List<String> vids;

    @Nullable
    @Loggable
    private List<String> types;

    @Nullable
    @Loggable
    private List<String> sources;

    @Nullable
    @Loggable
    private List<String> iins;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativesActiveRequest that = (RelativesActiveRequest) o;
        return Objects.equals(iin, that.iin) &&
                Objects.equals(dateFrom, that.dateFrom) &&
                Objects.equals(dateTo, that.dateTo) &&
                Objects.equals(years, that.years) &&
                Objects.equals(vids, that.vids) &&
                Objects.equals(types, that.types) &&
                Objects.equals(sources, that.sources) &&
                Objects.equals(iins, that.iins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iin, dateFrom, dateTo, years, vids, types, sources, iins);
    }
}
