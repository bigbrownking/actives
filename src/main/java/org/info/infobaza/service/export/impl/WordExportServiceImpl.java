package org.info.infobaza.service.export.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.info.infobaza.dto.request.ExportRequest;
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
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.enpf.IndustrialService;
import org.info.infobaza.service.export.WordExportService;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.service.relations.RelationService;
import org.info.infobaza.util.date.DateUtil;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static org.info.infobaza.constants.Dictionary.RU;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordExportServiceImpl implements WordExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    // === ШРИФТЫ ===
    private static final String FONT_FAMILY = "Times New Roman";
    private static final int TITLE_SIZE = 16;
    private static final int HEADER_SIZE = 13;
    private static final int SUBHEADER_SIZE = 11;
    private static final int BODY_SIZE = 11;
    private static final int TABLE_SIZE = 9;

    @Override
    public void exportToWord(OutputStream outputStream, ExportRequest request) throws IOException {
        log.info("Starting Word export for IIN: {}", request.getIin());

        String dateFrom = request.getDateFrom() != null ? request.getDateFrom().toString() : null;
        String dateTo = request.getDateTo() != null ? request.getDateTo().toString() : null;
        String mainIin = request.getIin();
        List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
        List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();

        if (mainIin == null || mainIin.isEmpty() || dateFrom == null || dateTo == null) {
            throw new IllegalArgumentException("IIN and date range are required");
        }

        try (XWPFDocument document = new XWPFDocument()){
            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(mainIin);

            try {
                RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(mainIin, dateFrom, dateTo);
                if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                    List<String> relatedIins = primaryRelations.getTypeToRelation().values().stream()
                            .flatMap(List::stream)
                            .filter(r -> r.getLevel() == 1)
                            .map(RelationActive::getIin)
                            .filter(Objects::nonNull)
                            .filter(iin -> !iin.isEmpty())
                            .distinct()
                            .toList();
                    iinsToProcess.addAll(relatedIins);
                    log.info("Added {} related IINs", relatedIins.size());
                }
            } catch (Exception e) {
                log.error("Error fetching primary relations: {}", e.getMessage());
            }

            boolean contentAdded = false;
            for (int i = 0; i < iinsToProcess.size(); i++) {
                String iin = iinsToProcess.get(i);
                log.info("Processing IIN: {}", iin);

                try {
                    addTitle(document, "Данные для ИИН: " + iin);
                    contentAdded = true;

                    Person person = portretService.getPerson(iin);
                    RelationActiveWithTypes personPrimary = relationService.getPrimaryRelationsOfPerson(iin, dateFrom, dateTo);
                    RelationActiveWithTypes personSecondary = relationService.getSecondaryRelationsOfPerson(iin, dateFrom, dateTo);
                    List<Pension> pensions = enpfService.getPension(iin, dateFrom, dateTo);
                    Head head = headService.constructHead(iin, dateFrom, dateTo);
                    Industry industry = industrialService.getIndustry(iin);
                    List<TurnoverRecord> turnovers = enpfService.getTurnoverRecords(iin);

                    ActiveWithRecords activeResponse = (ActiveWithRecords) analyzer.getAllActivesOfPersonsByDates(
                            iin, dateFrom, dateTo, yearsActive, request.getVids(), request.getTypes(), request.getSources(), request.getIins());
                    IncomeWithRecords incomeResponse = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                            iin, dateFrom, dateTo, yearsIncome, request.getVids(), request.getSources(), request.getIins());

                    if (person != null) addPortraitSection(document, person);
                    addRelationsSection(document, personPrimary, personSecondary);
                    addActivesAndIncomesSection(document, activeResponse, incomeResponse);
                    addJobInformationSection(document, pensions, head, industry, turnovers);

                    if (i < iinsToProcess.size() - 1) {
                        XWPFParagraph breakPara = document.createParagraph();
                        XWPFRun run = breakPara.createRun();
                        run.addBreak(BreakType.PAGE);
                    }

                } catch (Exception e) {
                    log.error("Error processing IIN {}: {}", iin, e.getMessage());
                    addParagraph(document, "Ошибка при обработке данных для ИИН: " + iin, BODY_SIZE, false);
                    contentAdded = true;
                }
            }

            if (!contentAdded) {
                addParagraph(document, "Нет данных для ИИН: " + mainIin, BODY_SIZE, false);
            }

            document.write(outputStream);
            log.info("Word export completed");
        }
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(200);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(TITLE_SIZE);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
    }

    private void addHeader(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(100);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(HEADER_SIZE);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
    }

    private void addSubHeader(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(80);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(SUBHEADER_SIZE);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);
    }

    private void addParagraph(XWPFDocument doc, String text, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(60);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setBold(bold);
        run.setFontFamily(FONT_FAMILY);
    }

    private void addPortraitSection(XWPFDocument doc, Person person) {
        XWPFTable table = doc.createTable(1, 2);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);

        XWPFTableRow row = table.getRow(0);
        row.getCell(0).setWidth("15%");
        row.getCell(1).setWidth("85%");

        // Image
        XWPFTableCell imgCell = row.getCell(0);
        imgCell.removeParagraph(0);
        if (person.getImage() != null && !person.getImage().isEmpty()) {
            try {
                String imgData = person.getImage().replaceFirst("^data:image/[^;]+;base64,?", "");
                byte[] bytes = Base64.getDecoder().decode(imgData);
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    int picType = XWPFDocument.PICTURE_TYPE_JPEG;
                    if (person.getImage().contains("png")) picType = XWPFDocument.PICTURE_TYPE_PNG;
                    XWPFParagraph imgPara = imgCell.addParagraph();
                    XWPFRun imgRun = imgPara.createRun();
                    imgRun.addPicture(in, picType, "img.jpg", 100 * 9525, 100 * 9525);
                }
            } catch (Exception e) {
                log.error("Image error: {}", e.getMessage());
                imgCell.setText("");
            }
        } else {
            imgCell.setText("");
        }

        // Info
        XWPFTableCell infoCell = row.getCell(1);
        infoCell.removeParagraph(0);
        XWPFParagraph infoPara = infoCell.addParagraph();
        addRun(infoPara, "ФИО: " + (person.getFio() != null ? person.getFio() : "-"), BODY_SIZE);
        addRun(infoPara, "Возраст: " + (person.getAge() != 0 ? person.getAge() + " лет" : "Неизвестен"), BODY_SIZE);
        addRun(infoPara, "ИИН: " + (person.getIin() != null ? person.getIin() : "-"), BODY_SIZE);
        addRun(infoPara, "Портрет: " + (person.getPortret() != null ? String.join(", ", person.getPortret()) : "Отсутствует"), BODY_SIZE);
        addRun(infoPara, "Номинал: " + ((person.getIsNominal() != null && person.getIsNominal()) ? "Да" : "Нет"), BODY_SIZE);
        addRun(infoPara, "Подставной: " + ((person.getIsNominalUl() != null && person.getIsNominalUl()) ? "Да" : "Нет"), BODY_SIZE);
        addRun(infoPara, "Крипта: " + (person.getIsCryptoActive() ? "Есть" : "Нет"), BODY_SIZE);

        doc.createParagraph().setSpacingAfter(200);
    }

    private void addRun(XWPFParagraph p, String text, int size) {
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily(FONT_FAMILY);
        run.addBreak();
    }

    private void addRelationsSection(XWPFDocument doc, RelationActiveWithTypes primary, RelationActiveWithTypes secondary) {
        addHeader(doc, "Первичные связи:");
        if (primary != null && primary.getTypeToRelation() != null && !primary.getTypeToRelation().isEmpty()) {
            addRelationsTable(doc, primary.getTypeToRelation());
        } else {
            addParagraph(doc, "Нет первичных связей", BODY_SIZE, false);
        }

        addHeader(doc, "Вторичные связи:");
        if (secondary != null && secondary.getTypeToRelation() != null && !secondary.getTypeToRelation().isEmpty()) {
            addRelationsTable(doc, secondary.getTypeToRelation());
        } else {
            addParagraph(doc, "Нет вторичных связей", BODY_SIZE, false);
        }
        doc.createParagraph();
    }

    private void addRelationsTable(XWPFDocument doc, Map<String, List<RelationActive>> map) {
        for (Map.Entry<String, List<RelationActive>> entry : map.entrySet()) {
            String cat = entry.getKey();
            List<RelationActive> list = entry.getValue();

            addSubHeader(doc, cat + ":");

            if (list == null || list.isEmpty()) {
                addParagraph(doc, "  Нет связей", BODY_SIZE, false);
                continue;
            }

            XWPFTable table = doc.createTable(list.size() + 1, 7);
            table.setWidth("100%");

            // === УСТАНОВКА ШИРИНЫ КОЛОНОК ===
            CTTblWidth[] widths = new CTTblWidth[] {
                    createTblWidth("12%"),  // ФИО
                    createTblWidth("12%"),  // Связь
                    createTblWidth("12%"),  // ИИН
                    createTblWidth("10%"),  // Активы
                    createTblWidth("10%"),  // Доходы
                    createTblWidth("10%"),  // Сведения
                    createTblWidth("34%")   // Доп Инфо — САМАЯ ШИРОКАЯ
            };

            // Применяем к заголовку (все ячейки наследуют)
            XWPFTableRow header = table.getRow(0);
            for (int i = 0; i < 7; i++) {
                header.getCell(i).getCTTc().addNewTcPr().setTcW(widths[i]);
            }

            // Заголовки
            setCell(header.getCell(0), "ФИО", true);
            setCell(header.getCell(1), "Связь", true);
            setCell(header.getCell(2), "ИИН", true);
            setCell(header.getCell(3), "Активы", true);
            setCell(header.getCell(4), "Доходы", true);
            setCell(header.getCell(5), "Сведения", true);
            setCell(header.getCell(6), "Доп Инфо", true);

            int row = 1;
            for (RelationActive ra : list) {
                XWPFTableRow r = table.getRow(row++);

                // Применяем ширину и к строкам данных
                for (int i = 0; i < 7; i++) {
                    r.getCell(i).getCTTc().addNewTcPr().setTcW(widths[i]);
                }

                setCell(r.getCell(0), ra.getFio(), false);
                setCell(r.getCell(1), ra.getRelation(), false);
                setCell(r.getCell(2), ra.getIin(), false);
                setCell(r.getCell(3), ra.getActives(), false);
                setCell(r.getCell(4), ra.getIncomes(), false);
                setCell(r.getCell(5), ra.getInfo(), false);

                String dop = formatDopinfo(ra);
                setCell(r.getCell(6), dop, false);
            }
            doc.createParagraph();
        }
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

    private void addActivesAndIncomesSection(XWPFDocument doc, ActiveWithRecords active, IncomeWithRecords income) {
        addHeader(doc, "Активы:");
        if (active != null && active.getRecordsByOper() != null && !active.getRecordsByOper().isEmpty()) {
            addActivesTable(doc, active.getRecordsByOper());
        } else {
            addParagraph(doc, "Нет данных об активах", BODY_SIZE, false);
        }

        addHeader(doc, "Доходы:");
        if (income != null && income.getRecordsByYear() != null && !income.getRecordsByYear().isEmpty()) {
            addIncomesTable(doc, income.getRecordsByYear());
        } else {
            addParagraph(doc, "Нет данных о доходах", BODY_SIZE, false);
        }
    }

    private void addActivesTable(XWPFDocument doc, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            addParagraph(doc, "  Нет записей для активов", BODY_SIZE, false);
            doc.createParagraph();
            return;
        }

        boolean hasESF = records.stream().anyMatch(r -> r instanceof ESFInformationRecordDt);
        int cols = hasESF ? 9 : 6;

        XWPFTable table = doc.createTable(records.size() + 1, cols);
        table.setWidth("100%");

        // === ШИРИНЫ КОЛОНОК — ТОЧНО КАК В EXCEL ===
        CTTblWidth[] widths = hasESF
                ? new CTTblWidth[] {
                createTblWidth("12%"),  // ИИН/БИН
                createTblWidth("12%"),  // ИИН Покуп.
                createTblWidth("12%"),  // ИИН Прод.
                createTblWidth("10%"),  // Дата
                createTblWidth("12%"),  // База данных
                createTblWidth("12%"),  // Активы
                createTblWidth("12%"),  // Операция
                createTblWidth("18%"),  // Доп. инфо ← ШИРОКАЯ
                createTblWidth("10%")   // Сумма
        }
                : new CTTblWidth[] {
                createTblWidth("15%"),  // ИИН/БИН
                createTblWidth("12%"),  // Дата
                createTblWidth("15%"),  // База данных
                createTblWidth("15%"),  // Операция
                createTblWidth("33%"),  // Доп. инфо ← САМАЯ ШИРОКАЯ
                createTblWidth("10%")   // Сумма
        };

        // === ЗАГОЛОВОК ===
        XWPFTableRow header = table.getRow(0);
        if (hasESF) {
            setCellWithWidth(header.getCell(0), "ИИН/БИН", true, widths[0]);
            setCellWithWidth(header.getCell(1), "ИИН Покуп.", true, widths[1]);
            setCellWithWidth(header.getCell(2), "ИИН Прод.", true, widths[2]);
            setCellWithWidth(header.getCell(3), "Дата", true, widths[3]);
            setCellWithWidth(header.getCell(4), "База данных", true, widths[4]);
            setCellWithWidth(header.getCell(5), "Активы", true, widths[5]);
            setCellWithWidth(header.getCell(6), "Операция", true, widths[6]);
            setCellWithWidth(header.getCell(7), "Доп. инфо", true, widths[7]);
            setCellWithWidth(header.getCell(8), "Сумма", true, widths[8]);
        } else {
            setCellWithWidth(header.getCell(0), "ИИН/БИН", true, widths[0]);
            setCellWithWidth(header.getCell(1), "Дата", true, widths[1]);
            setCellWithWidth(header.getCell(2), "База данных", true, widths[2]);
            setCellWithWidth(header.getCell(3), "Операция", true, widths[3]);
            setCellWithWidth(header.getCell(4), "Доп. инфо", true, widths[4]);
            setCellWithWidth(header.getCell(5), "Сумма", true, widths[5]);
        }

        // === ДАННЫЕ ===
        int rowIndex = 1;
        for (RecordDt r : records) {
            XWPFTableRow row = table.getRow(rowIndex++);

            // Применяем ширину к каждой ячейке
            for (int i = 0; i < cols; i++) {
                CTTcPr tcPr = row.getCell(i).getCTTc().isSetTcPr() ? row.getCell(i).getCTTc().getTcPr() : row.getCell(i).getCTTc().addNewTcPr();
                tcPr.setTcW(widths[i]);
            }

            if (hasESF && r instanceof ESFInformationRecordDt esf) {
                setCellWithWidth(row.getCell(0), esf.getIin_bin(), false, widths[0]);
                setCellWithWidth(row.getCell(1), esf.getIin_bin_pokup(), false, widths[1]);
                setCellWithWidth(row.getCell(2), esf.getIin_bin_prod(), false, widths[2]);
                setCellWithWidth(row.getCell(3), formatDate(esf.getDate()), false, widths[3]);
                setCellWithWidth(row.getCell(4), esf.getDatabase(), false, widths[4]);
                setCellWithWidth(row.getCell(5), esf.getAktivy(), false, widths[5]);
                setCellWithWidth(row.getCell(6), esf.getOper(), false, widths[6]);
                setCellWithWidth(row.getCell(7), esf.getDopinfo(), false, widths[7]);
                setCellWithWidth(row.getCell(8), esf.getSumm(), false, widths[8]);
            } else {
                setCellWithWidth(row.getCell(0), r.getIin_bin(), false, widths[0]);
                setCellWithWidth(row.getCell(1), "-", false, widths[1]);
                setCellWithWidth(row.getCell(2), "-", false, widths[2]);
                setCellWithWidth(row.getCell(3), formatDate(r.getDate()), false, widths[3]);
                setCellWithWidth(row.getCell(4), r.getDatabase(), false, widths[4]);
                setCellWithWidth(row.getCell(5), r.getAktivy(), false, widths[5]);
                setCellWithWidth(row.getCell(6), r.getOper(), false, widths[6]);
                setCellWithWidth(row.getCell(7), r.getDopinfo(), false, widths[7]);
                setCellWithWidth(row.getCell(8), r.getSumm(), false, widths[8]);
            }
        }

        doc.createParagraph();
    }
    private void addIncomesTable(XWPFDocument doc, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            addParagraph(doc, "  Нет записей о доходах", BODY_SIZE, false);
            doc.createParagraph();
            return;
        }

        XWPFTable table = doc.createTable(records.size() + 1, 6);
        table.setWidth("100%");


        // === УСТАНОВКА ШИРИНЫ КОЛОНОК ===
        CTTblWidth[] widths = new CTTblWidth[] {
                createTblWidth("15%"),  // ИИН/БИН
                createTblWidth("12%"),  // Дата
                createTblWidth("15%"),  // База данных
                createTblWidth("15%"),  // Операция
                createTblWidth("33%"),  // Доп. инфо ← САМАЯ ШИРОКАЯ
                createTblWidth("10%")   // Сумма
        };

        XWPFTableRow header = table.getRow(0);
        setCellWithWidth(header.getCell(0), "ИИН/БИН", true, widths[0]);
        setCellWithWidth(header.getCell(1), "Дата", true, widths[1]);
        setCellWithWidth(header.getCell(2), "База данных", true, widths[2]);
        setCellWithWidth(header.getCell(3), "Операция", true, widths[3]);
        setCellWithWidth(header.getCell(4), "Доп. инфо", true, widths[4]);
        setCellWithWidth(header.getCell(5), "Сумма", true, widths[5]);

        int row = 1;
        for (RecordDt r : records) {
            XWPFTableRow tr = table.getRow(row++);

            // Применяем ширину к каждой строке
            for (int i = 0; i < 6; i++) {
                tr.getCell(i).getCTTc().addNewTcPr().setTcW(widths[i]);
            }

            setCellWithWidth(tr.getCell(0), r.getIin_bin(), false, widths[0]);
            setCellWithWidth(tr.getCell(1), formatDate(r.getDate()), false, widths[1]);
            setCellWithWidth(tr.getCell(2), r.getDatabase(), false, widths[2]);
            setCellWithWidth(tr.getCell(3), r.getOper(), false, widths[3]);
            setCellWithWidth(tr.getCell(4), r.getDopinfo(), false, widths[4]);
            setCellWithWidth(tr.getCell(5), r.getSumm(), false, widths[5]);
        }
        doc.createParagraph();
    }
    private void addJobInformationSection(XWPFDocument doc, List<Pension> pensions, Head head, Industry industry, List<TurnoverRecord> turnovers) {
        addHeader(doc, "Отрасль:");
        addParagraph(doc, industry != null && industry.getName() != null ? industry.getName() : "Отсутствует", BODY_SIZE, false);

        addHeader(doc, "Пенсионные взносы:");
        if (pensions != null && !pensions.isEmpty()) addPensionsTable(doc, pensions);
        else addParagraph(doc, "Нет данных", BODY_SIZE, false);

        if (head != null && !head.isEmpty()) addHeadTable(doc, head);
        else addParagraph(doc, "Нет данных", BODY_SIZE, false);

        addHeader(doc, "Банковские счета:");
        if (turnovers != null && !turnovers.isEmpty()) addTurnoversTable(doc, turnovers);
        else addParagraph(doc, "Нет данных", BODY_SIZE, false);
    }

    private void addHeadTable(XWPFDocument doc, Head head) {
        if (head.getHead() != null && !head.getHead().isEmpty()) {
            addSubHeader(doc, "Руководящие должности:");
            XWPFTable table = doc.createTable(head.getHead().size() + 1, 5);
            table.setWidth("100%");
            XWPFTableRow h = table.getRow(0);
            setCell(h.getCell(0), "ИИН/БИН", true);
            setCell(h.getCell(1), "Тип позиции", true);
            setCell(h.getCell(2), "ИИН/БИН налогопл.", true);
            setCell(h.getCell(3), "Подставной", true);
            setCell(h.getCell(4), "Наименование", true);

            int row = 1;
            for (SupervisorRecord s : head.getHead()) {
                XWPFTableRow r = table.getRow(row++);
                setCell(r.getCell(0), s.getIin_bin(), false);
                setCell(r.getCell(1), s.getPositionType(), false);
                setCell(r.getCell(2), s.getTaxpayer_iin_bin(), false);
                setCell(r.getCell(3), s.isNominal() ? "Да" : "Нет", false);
                setCell(r.getCell(4), s.getTaxpayerName(), false);
            }
        }

        addSubHeader(doc, "Финансовая информация:");
        addParagraph(doc, "Доход: " + (head.getIncome() != null ? head.getIncome() : "-"), BODY_SIZE, false);
        addParagraph(doc, "Налоги: " + (head.getTax() != null ? head.getTax() : "-"), BODY_SIZE, false);

        if (head.getEsf() != null && !head.getEsf().isEmpty()) {
            addSubHeader(doc, "ESF информация:");
            XWPFTable table = doc.createTable(head.getEsf().size() + 1, 4);
            table.setWidth("100%");
            XWPFTableRow h = table.getRow(0);
            setCell(h.getCell(0), "ИИН/БИН", true);
            setCell(h.getCell(1), "Дата", true);
            setCell(h.getCell(2), "Активы", true);
            setCell(h.getCell(3), "Сумма", true);
            int row = 1;
            for (var e : head.getEsf()) {
                XWPFTableRow r = table.getRow(row++);
                setCell(r.getCell(0), e.getIin_bin(), false);
                setCell(r.getCell(1), String.valueOf(e.getDate()), false);
                setCell(r.getCell(2), e.getAktivy(), false);
                setCell(r.getCell(3), String.valueOf(e.getSumm()), false);
            }
        }

        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            addSubHeader(doc, "Статусы:");
            addParagraph(doc, String.join(", ", head.getStatuses()), BODY_SIZE, false);
        }
    }

    private void addPensionsTable(XWPFDocument doc, List<Pension> list) {
        XWPFTable table = doc.createTable(list.size() + 1, 7);
        table.setWidth("100%");
        XWPFTableRow h = table.getRow(0);
        setCell(h.getCell(0), "Дата с", true);
        setCell(h.getCell(1), "Дата по", true);
        setCell(h.getCell(2), "Наименование", true);
        setCell(h.getCell(3), "P_RNN", true);
        setCell(h.getCell(4), "Макс. з.п.", true);
        setCell(h.getCell(5), "Последняя з.п.", true);
        setCell(h.getCell(6), "Суммарно", true);
        int row = 1;
        for (Pension p : list) {
            XWPFTableRow r = table.getRow(row++);
            setCell(r.getCell(0), p.getDateFrom(), false);
            setCell(r.getCell(1), p.getDateTo(), false);
            setCell(r.getCell(2), p.getName(), false);
            setCell(r.getCell(3), p.getP_RNN(), false);
            setCell(r.getCell(4), p.getMaxSalary(), false);
            setCell(r.getCell(5), p.getLastSalary(), false);
            setCell(r.getCell(6), p.getSumm(), false);
        }
    }

    private void addTurnoversTable(XWPFDocument doc, List<TurnoverRecord> list) {
        XWPFTable table = doc.createTable(list.size() + 1, 7);
        table.setWidth("100%");
        XWPFTableRow h = table.getRow(0);
        setCell(h.getCell(0), "ИИН/БИН", true);
        setCell(h.getCell(1), "Банк", true);
        setCell(h.getCell(2), "Счет", true);
        setCell(h.getCell(3), "Сумма", true);
        setCell(h.getCell(4), "Дата от", true);
        setCell(h.getCell(5), "Дата до", true);
        setCell(h.getCell(6), "Источник", true);
        int row = 1;
        for (TurnoverRecord t : list) {
            XWPFTableRow r = table.getRow(row++);
            setCell(r.getCell(0), t.getIinBin(), false);
            setCell(r.getCell(1), t.getBankName(), false);
            setCell(r.getCell(2), t.getBankAccount(), false);
            setCell(r.getCell(3), t.getSumm(), false);
            setCell(r.getCell(4), t.getStartDate(), false);
            setCell(r.getCell(5), t.getEndDate(), false);
            setCell(r.getCell(6), t.getSource(), false);
        }
    }

    private void setCell(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(bold ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);

        // Включаем перенос строк
        CTPPr pPr = p.getCTP().addNewPPr();
        pPr.addNewJc().setVal(STJc.LEFT);
        p.getCTP().addNewPPr().addNewWordWrap();

        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "-");
        run.setFontSize(bold ? SUBHEADER_SIZE : TABLE_SIZE);
        run.setBold(bold);
        run.setFontFamily(FONT_FAMILY);
    }
    private CTTblWidth createTblWidth(String width) {
        CTTblWidth tblWidth = CTTblWidth.Factory.newInstance();
        if (width.endsWith("%")) {
            tblWidth.setType(STTblWidth.PCT);
            tblWidth.setW(Integer.parseInt(width.replace("%", "")) * 50); // 1% = 50
        } else {
            tblWidth.setType(STTblWidth.DXA);
            tblWidth.setW(Integer.parseInt(width));
        }
        return tblWidth;
    }
    private void setCellWithWidth(XWPFTableCell cell, String text, boolean bold, CTTblWidth width) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(bold ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);

        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "-");
        run.setFontSize(bold ? SUBHEADER_SIZE : TABLE_SIZE);
        run.setBold(bold);
        run.setFontFamily(FONT_FAMILY);

        // Установка ширины колонки
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        tcPr.setTcW(width);
    }

    private String formatDate(Object date) {
        if (date == null) return "-";
        String str = date.toString();
        return str.length() > 10 ? str.substring(0, 10) : str;
    }
}