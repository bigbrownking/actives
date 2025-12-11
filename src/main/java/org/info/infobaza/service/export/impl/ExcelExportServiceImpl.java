package org.info.infobaza.service.export.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.request.MassExportRequest;
import org.info.infobaza.dto.response.info.active.ActiveWithRecords;
import org.info.infobaza.dto.response.info.income.IncomeWithRecords;
import org.info.infobaza.dto.response.job.Head;
import org.info.infobaza.dto.response.job.Industry;
import org.info.infobaza.dto.response.job.Pension;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.dto.response.relation.RelationActiveWithTypes;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.enpf.IndustrialService;
import org.info.infobaza.service.export.ExcelExportService;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.service.relations.RelationService;
import org.info.infobaza.util.date.DateUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.info.infobaza.constants.Dictionary.RU;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    @Override
    public void exportToExcel(OutputStream outputStream, ExportRequest request) throws IOException {
        long startTotal = System.currentTimeMillis();
        log.info("Starting exportToExcel()...");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Person Report");
            int rowIndex = 0;

            // === 1. Fetching request data ===
            String dateFrom = request.getDateFrom().toString();
            String dateTo = request.getDateTo().toString();
            String mainIin = request.getIin();
            List<String> yearsActive = request.getYearsActive() == null
                    ? dateUtil.getYears(dateFrom, dateTo)
                    : request.getYearsActive();
            List<String> yearsIncome = request.getYearsIncome() == null
                    ? dateUtil.getYears(dateFrom, dateTo)
                    : request.getYearsIncome();

            // === 2. Fetch primary relations for main IIN ===
            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(mainIin);

            RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                    mainIin, dateFrom, dateTo);

            if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                iinsToProcess.addAll(
                        primaryRelations.getTypeToRelation().values().stream()
                                .flatMap(List::stream)
                                .map(RelationActive::getIin)
                                .filter(Objects::nonNull)
                                .filter(s -> !s.isBlank())
                                .distinct()
                                .toList()
                );
            }

            log.info("Processing {} IIN(s)...", iinsToProcess.size());

            // === 3. Process each IIN ===
            for (int i = 0; i < iinsToProcess.size(); i++) {
                String iin = iinsToProcess.get(i);
                long tIinStart = System.currentTimeMillis();

                rowIndex = addBoldRow(sheet, rowIndex, "Данные для ИИН: " + iin);
                rowIndex++;

                Person person = portretService.getPerson(iin);
                RelationActiveWithTypes personPrimaryRelations = relationService.getPrimaryRelationsOfPerson(iin, dateFrom, dateTo);
                RelationActiveWithTypes personSecondaryRelations = relationService.getSecondaryRelationsOfPerson(iin, dateFrom, dateTo);
                List<Pension> pensions = enpfService.getPension(iin, dateFrom, dateTo);
                Head head = headService.constructHead(iin, dateFrom, dateTo);
                Industry industry = industrialService.getIndustry(iin);
                List<TurnoverRecord> turnoverRecords = enpfService.getTurnoverRecords(iin);

                ActiveWithRecords activeResponse = (ActiveWithRecords) analyzer.getAllActivesOfPersonsByDates(
                        iin, dateFrom, dateTo, yearsActive, request.getVids(), request.getTypes(),
                        request.getSources(), request.getIins());
                IncomeWithRecords incomeResponse = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                        iin, dateFrom, dateTo, yearsIncome, request.getVids(), request.getSources(),
                        request.getIins());

                rowIndex = addPortraitSection(workbook, sheet, rowIndex, person);
                rowIndex = addRelationsSection(sheet, rowIndex, personPrimaryRelations, personSecondaryRelations);
                rowIndex = addActivesAndIncomesSection(sheet, rowIndex, activeResponse, incomeResponse);
                rowIndex = addJobInformationSection(sheet, rowIndex, pensions, head, industry, turnoverRecords);

                if (i < iinsToProcess.size() - 1) rowIndex++;

                log.info("Completed IIN {} in {} ms", iin, (System.currentTimeMillis() - tIinStart));
            }

            for (int i = 0; i < 9; i++) sheet.autoSizeColumn(i);
            workbook.write(outputStream);
        }

        log.info("exportToExcel() finished in {} ms", (System.currentTimeMillis() - startTotal));
    }
    @Override
    public void exportToExcelMass(OutputStream outputStream, MassExportRequest request) throws IOException {
        long totalStart = System.currentTimeMillis();
        List<String> iins = request.getIins();
        String dateFrom = "1980-01-01";
        int endYear = LocalDate.now().getYear();
        String dateTo = endYear + "-12-31";
        List<String> yearsActive = dateUtil.getYears(dateFrom, dateTo);
        List<String> yearsIncome = dateUtil.getYears(dateFrom, dateTo);

        log.info("Starting mass export for {} main IIN(s)", iins.size());

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        Sheet sheet = workbook.createSheet("Person Report");
        int rowIndex = 0;
        Set<String> processedIINs = new HashSet<>();

        for (String iinInput : iins) {
            if (!processedIINs.add(iinInput)) {
                log.info("Skipping duplicate IIN: {}", iinInput);
                continue;
            }

            long iinInputStart = System.currentTimeMillis();
            log.info("Processing main IIN: {}", iinInput);

            // === 1. Получаем связи для основного IIN ===
            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(iinInput);

            long relStart = System.currentTimeMillis();
            try {
                RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(iinInput, dateFrom, dateTo);
                if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                    List<String> relatedIins = primaryRelations.getTypeToRelation().values().stream()
                            .flatMap(List::stream)
                            .filter(x -> x.getLevel() == 1)
                            .map(RelationActive::getIin)
                            .filter(Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .toList();
                    iinsToProcess.addAll(relatedIins);
                    log.info("Found {} related IINs for {}", relatedIins.size(), iinInput);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch relations for IIN {}: {}", iinInput, e.getMessage());
            }

            log.info("all iins: {}", iinsToProcess);
            // === 2. Обрабатываем каждый IIN последовательно ===
            for (String iin : iinsToProcess) {
                if (!processedIINs.add(iin)) {
                    log.info("Skipping already processed IIN: {}", iin);
                    continue;
                }

                long iinStart = System.currentTimeMillis();
                log.info("Starting data fetch for IIN: {}", iin);

                // === Последовательные вызовы (как в exportToExcel) ===
                Person person = portretService.getPerson(iin);
                RelationActiveWithTypes primary = relationService.getPrimaryRelationsOfPerson(iin, dateFrom, dateTo);
                RelationActiveWithTypes secondary = relationService.getSecondaryRelationsOfPerson(iin, dateFrom, dateTo);
                List<Pension> pensions = enpfService.getPension(iin, dateFrom, dateTo);
                Head headFl = headService.constructHead(iin, dateFrom, dateTo);
                Industry industry = industrialService.getIndustry(iin);
                List<TurnoverRecord> turnovers = enpfService.getTurnoverRecords(iin);
                ActiveWithRecords actives = (ActiveWithRecords) analyzer.getAllActivesOfPersonsByDates(
                        iin, dateFrom, dateTo, yearsActive, null, null, null, request.getIins());
                IncomeWithRecords incomes = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                        iin, dateFrom, dateTo, yearsIncome, null, null, request.getIins());

                long fetchEnd = System.currentTimeMillis();

                // === Запись в Excel ===
                long writeStart = System.currentTimeMillis();
                rowIndex = addBoldRow(sheet, rowIndex, "Данные для ИИН: " + iin);
                rowIndex++;
                rowIndex = addPortraitSection(workbook, sheet, rowIndex, person);
                rowIndex = addRelationsSection(sheet, rowIndex, primary, secondary);
                rowIndex = addActivesAndIncomesSection(sheet, rowIndex, actives, incomes);
                rowIndex = addJobInformationSection(sheet, rowIndex, pensions, headFl, industry, turnovers);
                rowIndex++;

                log.info("Completed IIN {} in {} ms", iin, (System.currentTimeMillis() - iinStart));
            }

            log.info("Completed main IIN {} in {} ms", iinInput, (System.currentTimeMillis() - iinInputStart));
        }

        // === Финальная запись ===
        long finalStart = System.currentTimeMillis();
        for (int i = 0; i < 9; i++) {
            sheet.setColumnWidth(i, 6000);
        }
        workbook.write(outputStream);
        workbook.close();
        workbook.dispose();

        long totalTime = (System.currentTimeMillis() - totalStart) / 1000;
        log.info("Mass export completed in {} seconds for {} unique IIN(s)", totalTime, processedIINs.size());
    }
    private int addPortraitSection(Workbook workbook, Sheet sheet, int rowIndex, Person person) {
        rowIndex = addBoldRow(sheet, rowIndex, "Портрет");

        Row row = sheet.createRow(rowIndex++);
        Cell infoCell = row.createCell(0);

        StringBuilder info = new StringBuilder();
        info.append("ФИО: ").append(orDash(person.getFio())).append("\n");
        info.append("Возраст: ").append(person.getAge() != 0 ? person.getAge() + " лет" : "Неизвестен").append("\n");
        info.append("ИИН: ").append(orDash(person.getIin())).append("\n");
        info.append("Портрет: ").append(person.getPortret() != null ? String.join(", ", person.getPortret()) : "Отсутствует").append("\n");
        info.append("Номинал: ").append(person.getIsNominal() != null && person.getIsNominal() ? "Да" : "Нет").append("\n");
        info.append("Подставной владелец: ").append(person.getIsNominalUl() != null && person.getIsNominalUl() ? "Да" : "Нет").append("\n");
        info.append("Крипта: ").append(person.getIsCryptoActive() ? "Есть" : "Нет");

        infoCell.setCellValue(info.toString());

        return rowIndex + 1;
    }

    private int addRelationsSection(Sheet sheet, int rowIndex, RelationActiveWithTypes primary, RelationActiveWithTypes secondary) {
        rowIndex = addBoldRow(sheet, rowIndex, "Первичные связи:");
        rowIndex = hasRelations(primary) ? addRelationsTable(sheet, rowIndex, primary.getTypeToRelation())
                : addRow(sheet, rowIndex, "Нет первичных связей");
        rowIndex++;

        rowIndex = addBoldRow(sheet, rowIndex, "Вторичные связи:");
        rowIndex = hasRelations(secondary) ? addRelationsTable(sheet, rowIndex, secondary.getTypeToRelation())
                : addRow(sheet, rowIndex, "Нет вторичных связей");
        return rowIndex + 1;
    }

    private int addRelationsTable(Sheet sheet, int rowIndex, Map<String, List<RelationActive>> relationsMap) {
        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            rowIndex = addBoldRow(sheet, rowIndex, category + ":");

            if (relations == null || relations.isEmpty()) {
                rowIndex = addRow(sheet, rowIndex, "  Нет связей");
                continue;
            }

            Row header = sheet.createRow(rowIndex++);
            setCell(header, 0, "ФИО", true);
            setCell(header, 1, "Связь", true);
            setCell(header, 2, "ИИН", true);
            setCell(header, 3, "Активы", true);
            setCell(header, 4, "Доходы", true);
            setCell(header, 5, "Сведения", true);
            setCell(header, 6, "Доп Инфо", true);

            for (RelationActive ra : relations) {
                Row r = sheet.createRow(rowIndex++);
                setCell(r, 0, orDash(ra.getFio()), false);
                setCell(r, 1, orDash(ra.getRelation()), false);
                setCell(r, 2, orDash(ra.getIin()), false);
                setCell(r, 3, orDash(ra.getActives()), false);
                setCell(r, 4, orDash(ra.getIncomes()), false);
                setCell(r, 5, orDash(ra.getInfo()), false);
                setCell(r, 6, formatDopinfo(ra), false);
            }
        }
        return rowIndex;
    }

    private int addActivesAndIncomesSection(Sheet sheet, int rowIndex, ActiveWithRecords activeResponse,
                                            IncomeWithRecords incomeResponse) {
        rowIndex = addBoldRow(sheet, rowIndex, "Активы:");
        rowIndex = activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()
                ? addActivesTable(sheet, rowIndex, activeResponse.getRecordsByOper())
                : addRow(sheet, rowIndex, "Нет данных об активах");
        rowIndex++;

        rowIndex = addBoldRow(sheet, rowIndex, "Доходы:");
        rowIndex = incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()
                ? addIncomesTable(sheet, rowIndex, incomeResponse.getRecordsByYear())
                : addRow(sheet, rowIndex, "Нет данных о доходах");
        return rowIndex + 1;
    }

    private int addActivesTable(Sheet sheet, int rowIndex, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return addRow(sheet, rowIndex, "  Нет записей для активов");
        }

        boolean hasESF = records.stream().anyMatch(r -> r instanceof ESFInformationRecordDt);
        Row header = sheet.createRow(rowIndex++);
        if (hasESF) {
            setCell(header, 0, "ИИН/БИН", true);
            setCell(header, 1, "ИИН Покуп.", true);
            setCell(header, 2, "ИИН Прод.", true);
            setCell(header, 3, "Дата", true);
            setCell(header, 4, "База данных", true);
            setCell(header, 5, "Активы", true);
            setCell(header, 6, "Операция", true);
            setCell(header, 7, "Доп. инфо", true);
            setCell(header, 8, "Сумма", true);

            for (RecordDt r : records) {
                Row row = sheet.createRow(rowIndex++);
                if (r instanceof ESFInformationRecordDt esf) {
                    setCell(row, 0, esf.getIin_bin(), false);
                    setCell(row, 1, esf.getIin_bin_pokup(), false);
                    setCell(row, 2, esf.getIin_bin_prod(), false);
                    setCell(row, 3, esf.getDate() != null ? esf.getDate().toString() : "-", false);
                    setCell(row, 4, esf.getDatabase(), false);
                    setCell(row, 5, esf.getAktivy(), false);
                    setCell(row, 6, esf.getOper(), false);
                    setCell(row, 7, esf.getDopinfo(), false);
                    setCell(row, 8, esf.getSumm(), false);
                } else {
                    setCell(row, 0, r.getIin_bin(), false);
                    setCell(row, 1, "-", false);
                    setCell(row, 2, "-", false);
                    setCell(row, 3, r.getDate() != null ? r.getDate().toString() : "-", false);
                    setCell(row, 4, r.getDatabase(), false);
                    setCell(row, 5, r.getAktivy(), false);
                    setCell(row, 6, r.getOper(), false);
                    setCell(row, 7, r.getDopinfo(), false);
                    setCell(row, 8, r.getSumm(), false);
                }
            }
        } else {
            setCell(header, 0, "ИИН/БИН", true);
            setCell(header, 1, "Дата", true);
            setCell(header, 2, "База данных", true);
            setCell(header, 3, "Операция", true);
            setCell(header, 4, "Доп. инфо", true);
            setCell(header, 5, "Сумма", true);

            for (RecordDt r : records) {
                Row row = sheet.createRow(rowIndex++);
                setCell(row, 0, r.getIin_bin(), false);
                setCell(row, 1, r.getDate() != null ? r.getDate().toString() : "-", false);
                setCell(row, 2, r.getDatabase(), false);
                setCell(row, 3, r.getOper(), false);
                setCell(row, 4, r.getDopinfo(), false);
                setCell(row, 5, r.getSumm(), false);
            }
        }
        return rowIndex + 1;
    }

    private int addIncomesTable(Sheet sheet, int rowIndex, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return addRow(sheet, rowIndex, "  Нет записей о доходах");
        }

        Row header = sheet.createRow(rowIndex++);
        setCell(header, 0, "ИИН/БИН", true);
        setCell(header, 1, "Дата", true);
        setCell(header, 2, "База данных", true);
        setCell(header, 3, "Операция", true);
        setCell(header, 4, "Доп. инфо", true);
        setCell(header, 5, "Сумма", true);

        for (RecordDt r : records) {
            Row row = sheet.createRow(rowIndex++);
            setCell(row, 0, r.getIin_bin(), false);
            setCell(row, 1, r.getDate() != null ? r.getDate().toString() : "-", false);
            setCell(row, 2, r.getDatabase(), false);
            setCell(row, 3, r.getOper(), false);
            setCell(row, 4, r.getDopinfo(), false);
            setCell(row, 5, r.getSumm() != null ? r.getSumm().toString() : "-", false);
        }
        return rowIndex;
    }

    private int addJobInformationSection(Sheet sheet, int rowIndex, List<Pension> pensions, Head headFl,
                                         Industry industry, List<TurnoverRecord> turnovers) {
        rowIndex = addBoldRow(sheet, rowIndex, "Отрасль:");
        rowIndex = addRow(sheet, rowIndex, industry != null && industry.getName() != null ? industry.getName() : "Информация отсутствует");
        rowIndex++;

        rowIndex = addBoldRow(sheet, rowIndex, "Пенсионные взносы:");
        rowIndex = pensions != null && !pensions.isEmpty() ? addPensionsTable(sheet, rowIndex, pensions)
                : addRow(sheet, rowIndex, "Нет данных");
        rowIndex++;

        rowIndex = headFl != null && !headFl.isEmpty() ? addHeadInformationTable(sheet, rowIndex, headFl)
                : addRow(sheet, rowIndex, "Нет информации");
        rowIndex++;

        rowIndex = addBoldRow(sheet, rowIndex, "Банковские счета:");
        rowIndex = turnovers != null && !turnovers.isEmpty() ? addTurnoversTable(sheet, rowIndex, turnovers)
                : addRow(sheet, rowIndex, "Нет информации");
        return rowIndex + 1;
    }

    private int addHeadInformationTable(Sheet sheet, int rowIndex, Head headFl) {
        if (headFl.getHead() != null && !headFl.getHead().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "Руководящие должности:");
            Row h = sheet.createRow(rowIndex++);
            setCell(h, 0, "ИИН/БИН", true);
            setCell(h, 1, "Тип позиции", true);
            setCell(h, 2, "ИИН/БИН налогопл.", true);
            setCell(h, 3, "Подставной владелец", true);
            setCell(h, 4, "Наименование", true);

            for (var s : headFl.getHead()) {
                Row r = sheet.createRow(rowIndex++);
                setCell(r, 0, s.getIin_bin(), false);
                setCell(r, 1, s.getPositionType(), false);
                setCell(r, 2, s.getTaxpayer_iin_bin(), false);
                setCell(r, 3, s.isNominal() ? "Да" : "Нет", false);
                setCell(r, 4, s.getTaxpayerName(), false);
            }
        }

        rowIndex = addBoldRow(sheet, rowIndex, "Финансовая информация:");
        rowIndex = addRow(sheet, rowIndex, "Доход: " + orDash(headFl.getIncome()));
        rowIndex = addRow(sheet, rowIndex, "Налоги: " + orDash(headFl.getTax()));

        if (headFl.getEsf() != null && !headFl.getEsf().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "ESF информация:");
            Row h = sheet.createRow(rowIndex++);
            setCell(h, 0, "ИИН/БИН", true);
            setCell(h, 1, "Дата", true);
            setCell(h, 2, "Активы", true);
            setCell(h, 3, "Сумма", true);

            for (var e : headFl.getEsf()) {
                Row r = sheet.createRow(rowIndex++);
                setCell(r, 0, e.getIin_bin(), false);
                setCell(r, 1, e.getDate() != null ? e.getDate().toString() : "-", false);
                setCell(r, 2, e.getAktivy(), false);
                setCell(r, 3, e.getSumm() != null ? e.getSumm().toString() : "-", false);
            }
        }

        if (headFl.getStatuses() != null && !headFl.getStatuses().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "Статусы:");
            rowIndex = addRow(sheet, rowIndex, String.join(", ", headFl.getStatuses()));
        }
        return rowIndex;
    }

    private int addPensionsTable(Sheet sheet, int rowIndex, List<Pension> pensions) {
        Row h = sheet.createRow(rowIndex++);
        setCell(h, 0, "Дата с", true);
        setCell(h, 1, "Дата по", true);
        setCell(h, 2, "Наименование", true);
        setCell(h, 3, "P_RNN", true);
        setCell(h, 4, "Макс. з.п.", true);
        setCell(h, 5, "Последняя з.п.", true);
        setCell(h, 6, "Суммарно", true);

        for (Pension p : pensions) {
            Row r = sheet.createRow(rowIndex++);
            setCell(r, 0, p.getDateFrom(), false);
            setCell(r, 1, p.getDateTo(), false);
            setCell(r, 2, p.getName(), false);
            setCell(r, 3, p.getP_RNN(), false);
            setCell(r, 4, p.getMaxSalary(), false);
            setCell(r, 5, p.getLastSalary(), false);
            setCell(r, 6, p.getSumm(), false);
        }
        return rowIndex;
    }

    private int addTurnoversTable(Sheet sheet, int rowIndex, List<TurnoverRecord> records) {
        Row h = sheet.createRow(rowIndex++);
        setCell(h, 0, "ИИН/БИН", true);
        setCell(h, 1, "Банк", true);
        setCell(h, 2, "Счет", true);
        setCell(h, 3, "Сумма", true);
        setCell(h, 4, "Дата от", true);
        setCell(h, 5, "Дата до", true);
        setCell(h, 6, "Источник", true);

        for (TurnoverRecord t : records) {
            Row r = sheet.createRow(rowIndex++);
            setCell(r, 0, t.getIinBin(), false);
            setCell(r, 1, t.getBankName(), false);
            setCell(r, 2, t.getBankAccount(), false);
            setCell(r, 3, t.getSumm(), false);
            setCell(r, 4, t.getStartDate(), false);
            setCell(r, 5, t.getEndDate(), false);
            setCell(r, 6, t.getSource(), false);
        }
        return rowIndex;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private int addBoldRow(Sheet sheet, int rowIndex, String text) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        cell.setCellStyle(style);
        return rowIndex + 1;
    }

    private int addRow(Sheet sheet, int rowIndex, String text) {
        Row row = sheet.createRow(rowIndex++);
        setCell(row, 0, text, false);
        return rowIndex;
    }

    private void setCell(Row row, int col, Object value, boolean bold) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "-" : value.toString());
        CellStyle style = row.getSheet().getWorkbook().createCellStyle();
        Font font = row.getSheet().getWorkbook().createFont();
        font.setBold(bold);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private String orDash(Object value) {
        return value == null || (value instanceof String s && s.isBlank()) ? "-" : value.toString();
    }

    private String formatDopinfo(RelationActive ra) {
        if (ra.getDopinfo() == null || ra.getDopinfo().isEmpty()) return "-";

        Map<String, List<String>> splitted = new LinkedHashMap<>();
        ra.getDopinfo().forEach((k, v) -> {
            if (!"vid_sviazi".equals(k)) {
                splitted.put(k, v == null ? List.of() : Arrays.stream(v.split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList());
            }
        });

        int max = splitted.values().stream().mapToInt(List::size).max().orElse(0);
        if (max == 0) return "-";

        List<String> lines = new ArrayList<>();
        List<String> dates = splitted.getOrDefault("Operation date", List.of());
        List<String> sums = splitted.getOrDefault("Operation summ", List.of());
        List<String> purposes = splitted.getOrDefault("Operation name", List.of());

        for (int i = 0; i < max; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append((i + 1)).append(". ");

            String rel = ra.getRelation();
            if (rel.contains("Поступили")) sb.append("Поступили ДС");
            else if (rel.contains("Перечислил")) sb.append("Перечислил ДС");
            else sb.append(rel.split(" \\(")[0]);

            if (i < dates.size() && !dates.get(i).isBlank()) {
                String d = dates.get(i).substring(0, 10);
                sb.append(" • ").append(d.substring(8)).append(".")
                        .append(d.substring(5, 7)).append(".")
                        .append(d.substring(0, 4));
            }

            if (i < sums.size() && !sums.get(i).isBlank()) {
                String sum = sums.get(i).replaceAll("\\D", "");
                if (!sum.isEmpty()) {
                    String formatted = String.format("%,d", Long.parseLong(sum)).replace(",", " ");
                    sb.append(" • ").append(formatted).append(" ₸");
                }
            }

            if (i < purposes.size() && !purposes.get(i).isBlank()) {
                sb.append(" • ").append(purposes.get(i));
            }

            List<String> order = List.of("AP code", "Registration date", "End registration date", "GRNZ", "Save begin date",
                    "Save end date", "VIN code", "Summ", "For", "Number", "Tax for", "BVU", "Tax number", "UGD", "KNP", "KBK",
                    "Purpose of tax", "For_dover", "Registration_date");

            for (String key : order) {
                if (splitted.containsKey(key) && i < splitted.get(key).size()) {
                    String val = splitted.get(key).get(i);
                    if (!val.isBlank()) {
                        sb.append(" • ").append(RU.getOrDefault(key, key)).append(": ").append(val);
                    }
                }
            }

            lines.add(sb.toString());
        }

        return String.join("\n", lines);
    }

    private boolean hasRelations(RelationActiveWithTypes rel) {
        return rel != null && rel.getTypeToRelation() != null && !rel.getTypeToRelation().isEmpty();
    }
}