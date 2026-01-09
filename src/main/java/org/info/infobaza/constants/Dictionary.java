package org.info.infobaza.constants;


import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.request.MassExportRequest;
import org.info.infobaza.dto.response.info.ServiceSources;
import org.info.infobaza.service.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public final class Dictionary {
    private static ServiceSources serviceSources;
    private static Set<String> incomeSourcesSet = new HashSet<>();
    private static Set<String> activeSourcesSet = new HashSet<>();

    private final FetcherRegistry fetcherRegistry;
    public static final Map<String, String> SECONDARY_STATUSES = Map.of(
            "Перечислил ДС", "Финансовые операции",
            "Поступили ДС", "Финансовые операции",
            "Вместе летали 3 и более раз", "Самолёт",
            "Вместе работали в 3 и более ЮЛ", "Работа",
            "Налоги", "Налоги",
            "Совместные автостраховки", "Общий страховой полис",
            "Вместе жили в 2 и более адресах", "Вместе жили и коммунальные платежи",
            "Коммунальные платежи", "Вместе жили и коммунальные платежи",
            "Поверенный", "Доверенность",
            "Доверитель", "Доверенность"
    );
    public static final Map<String, String> RU = Map.ofEntries(
            // Совместное проживание / работа
            Map.entry("AP code", "Код АП"),
            Map.entry("Registration date", "Дата регистрации"),
            Map.entry("End registration date", "Дата снятия с регистрации"),

            // Автостраховки
            Map.entry("GRNZ", "ГРНЗ"),
            Map.entry("Save begin date", "Начало страхования"),
            Map.entry("Save end date", "Окончание страхования"),
            Map.entry("VIN code", "VIN-код"),

            // Денежные переводы
            Map.entry("Operation date", "Дата операции"),
            Map.entry("Operation summ", "Сумма"),
            Map.entry("Operation name", "Операция"),

            // Коммунальные платежи
            Map.entry("Summ", "Сумма"),
            Map.entry("For", "За что"),
            Map.entry("Number", "Номер лицевого счёта"),

            // Налоги
            Map.entry("Tax for", "Налог за"),
            Map.entry("BVU", "Банк"),
            Map.entry("Tax number", "Номер платежа"),
            Map.entry("UGD", "УГД"),
            Map.entry("KNP", "КНП"),
            Map.entry("KBK", "КБК"),
            Map.entry("Purpose of tax", "Назначение налога"),

            // Доверенности
            Map.entry("For_dover", "По доверенности"),
            Map.entry("Registration_date", "Дата выдачи")
    );
    public static final Map<String, String[]> TYPE_PREFIXES = Map.ofEntries(
            Map.entry("Недвижимое имущество", new String[]{"Вид недвижимости:", "Описание:"}),
            Map.entry("Транспортные средства", new String[]{"Авто:", "Описание:"}),
            Map.entry("ГКБ-Транспортные средства", new String[]{"Авто:", "Описание:"}),
            Map.entry("Административный штраф", new String[]{"Был совершен штраф:"}),
            Map.entry("Животные", new String[]{"Описание:"}),
            Map.entry("Предметы исскуства", new String[]{"Описание:"}),
            Map.entry("Прочие активы", new String[]{"Описание:", "Наименование и коды стран:"}),
            Map.entry("ЖД составы", new String[]{"Тип жд состава:"}),
            Map.entry("Ценные бумаги", new String[]{"Намиенование акции:"}),
            Map.entry("Спецтехника", new String[]{"Вид:"}),
            Map.entry("Воздушные судна", new String[]{"Тип ВС:"}),
            Map.entry("ЮЛ", new String[]{"Намиенование ЮЛ:"}),
            Map.entry("Водный транспорт", new String[]{"Тип судна:"}),
            Map.entry("Иные имущества", new String[]{"Наименование и коды стран:"})
    );
    public static void generate_pdf_header(ExportRequest request,
                                           HttpServletResponse response){
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=person_" + request.getIin() + ".pdf");
    }

    public static void generate_excel_header(Object request, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename;

        if (request instanceof ExportRequest exportRequest) {
            filename = "person_" + exportRequest.getIin() + ".xlsx";
        } else if (request instanceof MassExportRequest) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            filename = "mass_export_" + timestamp + ".xlsx";
        } else {
            filename = "export.xlsx";
        }

        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
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

        Map<String, List<DataFetcher>> allActiveFetchers = fetcherRegistry.getAllActiveFetchersBySource();
        Map<String, List<DataFetcher>> allIncomeFetchers = fetcherRegistry.getAllIncomeFetchersBySource();

        for (Map.Entry<String, List<DataFetcher>> entry : allActiveFetchers.entrySet()) {
            activeSources.add(entry.getKey());

            for (DataFetcher fetcher : entry.getValue()) {
                FetcherMetadata meta = fetcher.getMetadata();
                activeTypes.addAll(Arrays.asList(meta.getTypes()));
                activeVids.addAll(Arrays.asList(meta.getVids()));
            }
        }

        for (Map.Entry<String, List<DataFetcher>> entry : allIncomeFetchers.entrySet()) {
            incomeSources.add(entry.getKey());

            for (DataFetcher fetcher : entry.getValue()) {
                FetcherMetadata meta = fetcher.getMetadata();
                incomeVids.addAll(Arrays.asList(meta.getVids()));
            }
        }

        incomeSourcesSet = new HashSet<>(incomeSources);
        activeSourcesSet = new HashSet<>(activeSources);

        return new ServiceSources(
                new ArrayList<>(incomeSources),
                new ArrayList<>(activeSources),
                new ArrayList<>(activeTypes),
                new ArrayList<>(activeVids),
                new ArrayList<>(incomeVids)
        );
    }
    public static Map<String, Set<String>> getIncomeMethodsBySource() {
        return incomeSourcesSet.stream()
                .collect(Collectors.toMap(
                        source -> source,
                        source -> Collections.emptySet()
                ));
    }

    public static Map<String, Set<String>> getActiveMethodsBySource() {
        return activeSourcesSet.stream()
                .collect(Collectors.toMap(
                        source -> source,
                        source -> Collections.emptySet()
                ));
    }
    @PostConstruct
    public void init() {
        serviceSources = getDistinctSources();
    }

    public List<String> getVidIncome() {
        return serviceSources.getIncomeVids();
    }

    public List<String> getVidActive() {
        return serviceSources.getActiveVids();
    }

    public List<String> getSourcesActive() {
        return serviceSources.getActiveSources();
    }

    public List<String> getSourcesIncome() {
        return serviceSources.getIncomeSources();
    }

    public List<String> getTypesActives() {
        return serviceSources.getActiveTypes();
    }
}
