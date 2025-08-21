package org.info.infobaza.util.date;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DateUtil {
    public List<String> getYears(String dateFrom, String dateTo) {
        if (dateFrom == null || dateTo == null) {
            return new ArrayList<>();
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(dateFrom, formatter);
            LocalDate endDate = LocalDate.parse(dateTo, formatter);

            int startYear = startDate.getYear();
            int endYear = endDate.getYear();

            List<String> years = new ArrayList<>();
            for (int year = startYear; year <= endYear; year++) {
                years.add(String.valueOf(year));
            }

            return years;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
