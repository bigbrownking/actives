package org.info.infobaza.util.convert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Slf4j
public class CustomBeanUtils extends BeanUtils {
    public static void copyPropertiesIgnoreNullPropertyNames(Object source, Object target, String... ignoreProperties) {
        String[] nullPropertyNames = getNullPropertyNames(source);
        String[] resultArray = null;
        if (ignoreProperties == null) {
            resultArray = nullPropertyNames;
        } else {
            try {
                resultArray = Arrays.stream(Stream
                                .concat(Arrays.stream(ignoreProperties), Arrays.stream(nullPropertyNames))
                                .toArray(String[]::new))
                        .distinct()
                        .toArray(String[]::new);
            } catch (Exception e) {
                log.info("Exception : " + e.getMessage());
            }
        }

        copyProperties(source, target, resultArray);
    }
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

}
