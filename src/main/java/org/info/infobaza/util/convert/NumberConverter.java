package org.info.infobaza.util.convert;

import org.info.infobaza.dto.response.person.Age;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

@Component
public class NumberConverter {

    public String formatNumber(double number) {
        long roundedNumber = (long) Math.ceil(number);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(roundedNumber);
    }

    public String formatNumber(String number) {
        try {
            double parsedNumber = Double.parseDouble(number.trim());
            long roundedNumber = (long) Math.ceil(parsedNumber);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator(' ');
            DecimalFormat df = new DecimalFormat("#,##0", symbols);
            return df.format(roundedNumber);
        } catch (NumberFormatException e) {
            return number != null ? number.trim() : "0";
        }
    }

    public Age getAgeByDates(LocalDate[] dates) {
        if (dates == null || dates[0] == null) {
            return null;
        }

        LocalDate birthDate = dates[0];
        LocalDate deathDate = dates[1];
        LocalDate currentDate = LocalDate.now();

        int age;
        boolean status;

        if (deathDate != null && !deathDate.isAfter(currentDate)) {
            age = Period.between(birthDate, deathDate).getYears();
            status = false;
        } else {
            age = Period.between(birthDate, currentDate).getYears();
            status = true;
        }

        return Age.builder()
                .age(age)
                .status(status)
                .build();
    }
    public BigDecimal parseBigDecimalOrZero(String summ) {
        try {
            if (summ == null || summ.trim().isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(summ.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

}
