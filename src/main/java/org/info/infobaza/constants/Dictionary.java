package org.info.infobaza.constants;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.info.infobaza.dto.response.info.ServiceSources;
import org.info.infobaza.service.AbstractService;
import org.info.infobaza.service.ServiceMetadata;
import org.info.infobaza.util.ServiceMetadataCollector;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
@RequiredArgsConstructor
public final class Dictionary {
    private final ServiceMetadataCollector serviceMetadataCollector;

    private static Map<String, List<Method>> incomeMethodsBySource = new HashMap<>();
    private static Map<String, List<Method>> activeMethodsBySource = new HashMap<>();
    private static Map<String, AbstractService> serviceBeans = new HashMap<>();


    private final ApplicationContext applicationContext;

    public static final List<String> VID_INCOME = List.of("Доход по данным ЕНПФ", "Доход ИП", "СГД ЮЛ", "Доход по данным ФНО", "Денежные средства",
            "в.т.ч. Доход от ИП", "в.т.ч. Доход из источников за пределами РК", "в.т.ч. Имущественный доход",
            "Доход от осуществления нотариуса, судебного исполнителя, адвоката, профессионального медиатора", "в.т.ч. Доход лица занимающиеся частной практикой");
    public static final List<String> VID_ACTIVE = List.of("Ценные бумаги", "Земельный участок", "Недвижимое имущество", "Транспортные средства", "ЕНИС НОТАРИУС", "Цифровые активы",
            "Сейфовые ячейки", "Денежные средства", "Иные имущества", "ЮЛ", "Спецтехника", "Воздушные судна", "Водный транспорт", "ЖД составы",
            "Животные", "Золото", "Украшения", "Предметы исскуства", "Прочие активы");
    public static final List<String> SOURCES_ACTIVE = List.of("Центральный депозитарий", "FM", "ЦУЛС", "FM-1", "FNO250", "FNO270", "ГКБ-Cведения по страховке Авто", "КАП МВД-Cведения по владельцам Авто",
            "ЕНИС НОТАРИУС", "FNO240", "СВедения КГД МФ РК", "Сведения МСХ", "Сведения Минтранспорта", "ESF", "ЕИАС", "НАО ЦОН");
    public static final List<String> SOURCES_INCOME = List.of("ЕНПФ", "FNO", "FNO270", "FNO200_05", "FNO240");
    public static final List<String> TYPES_ACTIVES = List.of("Реализация", "Приобретение", "Наличие");

    public static final Map<String, String> SECONDARY_STATUSES = Map.of("Отправил ДС", "ДС",
            "Получил ДС", "ДС",
            "Вместе летали 3 и более раз", "Самолёт",
            "Вместе работали в 3 и более ЮЛ", "Работа",
            "Налоги", "Налоги",
            "Совместные автостраховки", "Совместная страховка",
            "Вместе жили в 2 и более адресах", "Вместе жили и коммунальные платежи",
            "Коммунальные платежи", "Вместе жили и коммунальные платежи"
    );

    public ServiceSources get(){
        return serviceMetadataCollector.getDistinctSources();
    }




    @PostConstruct
    public void init() {
        serviceBeans = applicationContext.getBeansOfType(AbstractService.class);
        incomeMethodsBySource = new HashMap<>();
        activeMethodsBySource = new HashMap<>();

        for (AbstractService service : serviceBeans.values()) {
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

    public static Map<String, AbstractService> getServiceBeans() {
        return Collections.unmodifiableMap(serviceBeans);
    }

    public List<String> getVidIncome(){
        return serviceMetadataCollector.getDistinctSources().getIncomeVids();
    }
    public List<String> getVidActive(){
        return serviceMetadataCollector.getDistinctSources().getActiveVids();
    }
    public List<String> getSourcesActive(){
        return serviceMetadataCollector.getDistinctSources().getActiveSources();
    }
    public List<String> getSourcesIncome(){
        return serviceMetadataCollector.getDistinctSources().getIncomeSources();
    }
    public List<String> getTypesActives(){
        return serviceMetadataCollector.getDistinctSources().getActiveTypes();
    }
}
