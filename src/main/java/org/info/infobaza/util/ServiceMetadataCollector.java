package org.info.infobaza.util;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.response.info.ServiceSources;
import org.info.infobaza.service.AbstractService;
import org.info.infobaza.service.ServiceMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ServiceMetadataCollector {

    private final ApplicationContext applicationContext;

    public ServiceSources getDistinctSources() {
        Set<String> incomeSources = new HashSet<>();
        Set<String> activeSources = new HashSet<>();
        Set<String> activeTypes = new HashSet<>();
        Set<String> activeVids = new HashSet<>();
        Set<String> incomeVids = new HashSet<>();

        Map<String, AbstractService> serviceBeans = applicationContext.getBeansOfType(AbstractService.class);

        for (AbstractService service : serviceBeans.values()) {
            Method[] methods = service.getClass().getDeclaredMethods();
            for (Method method : methods) {
                ServiceMetadata annotation = method.getAnnotation(ServiceMetadata.class);
                if (annotation != null) {
                    String source = annotation.source().length > 0 ? annotation.source()[0] : "";

                    if (annotation.isActive()) {
                        activeSources.add(source);
                        activeTypes.addAll(Arrays.asList(annotation.type()));
                        activeVids.addAll(Arrays.asList(annotation.vids()));
                    }
                    if (annotation.isIncome()) {
                        incomeSources.add(source);
                        incomeVids.addAll(Arrays.asList(annotation.vids()));
                    }
                }
            }
        }

        return new ServiceSources(
                new ArrayList<>(incomeSources),
                new ArrayList<>(activeSources),
                new ArrayList<>(activeTypes),
                new ArrayList<>(activeVids),
                new ArrayList<>(incomeVids)
        );
    }
}