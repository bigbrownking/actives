package org.info.infobaza.util.date;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DateUtil {
    private final String PORTRET_DATE_FORMAT = "yyyy-MM-dd";
    private final String AGE_DATE_FORMAT = "yyyy/MM/dd";
    private final String OUTPUT_FORMAT = "dd-MM-yyyy";
    private final String TURNOVER_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final String OUTPUT_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private final DateTimeFormatter output_formatter = DateTimeFormatter.ofPattern(OUTPUT_FORMAT);
    private final DateTimeFormatter output_time_formatter = DateTimeFormatter.ofPattern(OUTPUT_TIME_FORMAT);
    private final DateTimeFormatter turnover_time_formatter = DateTimeFormatter.ofPattern(TURNOVER_TIME_FORMAT);
    private final DateTimeFormatter portret_formatter = DateTimeFormatter.ofPattern(PORTRET_DATE_FORMAT);
    private final DateTimeFormatter age_formatter = DateTimeFormatter.ofPattern(AGE_DATE_FORMAT);

    public List<String> getYears(String dateFrom, String dateTo) {
        if (dateFrom == null || dateTo == null) {
            return new ArrayList<>();
        }

        try {
            LocalDate startDate = LocalDate.parse(dateFrom, portret_formatter);
            LocalDate endDate = LocalDate.parse(dateTo, portret_formatter);

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

    public LocalDate formatPortretDate(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return LocalDate.parse(str, portret_formatter);
    }

    public LocalDate formatAgeDate(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return LocalDate.parse(str, age_formatter);
    }

    public String formatOutput(LocalDate localDate) {
        return localDate.format(output_formatter);
    }

    public String formatTimeToCustom(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(isoDate, turnover_time_formatter);
            return dateTime.format(output_time_formatter);
        } catch (Exception e) {
            return null;
        }
    }
    public LocalDate parseOutputDate(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(str, output_formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
