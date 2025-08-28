package org.info.infobaza.constants;


import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.response.info.ServiceSources;
import org.info.infobaza.service.InformationalService;
import org.info.infobaza.service.ServiceMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
@RequiredArgsConstructor
public final class Dictionary {
    private static Map<String, List<Method>> incomeMethodsBySource = new HashMap<>();
    private static Map<String, List<Method>> activeMethodsBySource = new HashMap<>();
    private static Map<String, InformationalService> serviceBeans = new HashMap<>();

    private final ApplicationContext applicationContext;
    private ServiceSources serviceSources;
    public static final Map<String, String> SECONDARY_STATUSES = Map.of("Отправил ДС", "ДС",
            "Получил ДС", "ДС",
            "Вместе летали 3 и более раз", "Самолёт",
            "Вместе работали в 3 и более ЮЛ", "Работа",
            "Налоги", "Налоги",
            "Совместные автостраховки", "Совместная страховка",
            "Вместе жили в 2 и более адресах", "Вместе жили и коммунальные платежи",
            "Коммунальные платежи", "Вместе жили и коммунальные платежи",
            "Доверенность", "Доверенность"
    );
    public static void generate_pdf_header(ExportRequest request,
                                           HttpServletResponse response){
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=person_" + request.getIin() + ".pdf");
    }

    public static void generate_excel_header(ExportRequest request,
                                           HttpServletResponse response){
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=person_" + request.getIin() + ".xlsx");
    }

    public static void generate_word_header(ExportRequest request,
                                           HttpServletResponse response){
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=person_" + request.getIin() + ".docx");
    }

    private ServiceSources getDistinctSources() {
        Set<String> incomeSources = new HashSet<>();
        Set<String> activeSources = new HashSet<>();
        Set<String> activeTypes = new HashSet<>();
        Set<String> activeVids = new HashSet<>();
        Set<String> incomeVids = new HashSet<>();

        Map<String, InformationalService> serviceBeans = applicationContext.getBeansOfType(InformationalService.class);

        for (InformationalService service : serviceBeans.values()) {
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


    @PostConstruct
    public void init() {
        serviceSources = getDistinctSources();
        serviceBeans = applicationContext.getBeansOfType(InformationalService.class);
        incomeMethodsBySource = new HashMap<>();
        activeMethodsBySource = new HashMap<>();

        for (InformationalService service : serviceBeans.values()) {
            for (Method method : service.getClass().getDeclaredMethods()) {
                ServiceMetadata metadata = method.getAnnotation(ServiceMetadata.class);
                if (metadata != null) {
                    if (metadata.isIncome()) {
                        for (String source : metadata.source()) {
                            incomeMethodsBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(method);
                        }
                    } else if (metadata.isActive()) {
                        for (String source : metadata.source()) {
                            activeMethodsBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(method);
                        }
                    }
                }
            }
        }
    }
    public static Map<String, List<Method>> getIncomeMethodsBySource() {
        return Collections.unmodifiableMap(incomeMethodsBySource);
    }

    public static Map<String, List<Method>> getActiveMethodsBySource() {
        return Collections.unmodifiableMap(activeMethodsBySource);
    }

    public static Map<String, InformationalService> getServiceBeans() {
        return Collections.unmodifiableMap(serviceBeans);
    }

    public List<String> getVidIncome(){
        return serviceSources.getIncomeVids();
    }
    public List<String> getVidActive(){
        return serviceSources.getActiveVids();
    }
    public List<String> getSourcesActive(){
        return serviceSources.getActiveSources();
    }
    public List<String> getSourcesIncome(){
        return serviceSources.getIncomeSources();
    }
    public List<String> getTypesActives(){
        return serviceSources.getActiveTypes();
    }
}
