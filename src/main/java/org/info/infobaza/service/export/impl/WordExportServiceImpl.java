package org.info.infobaza.service.export.impl;

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
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.adm_shtraf.AdministrationPayService;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.enpf.IndustrialService;
import org.info.infobaza.service.export.WordExportService;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.service.relations.RelationService;
import org.info.infobaza.util.date.DateUtil;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordExportServiceImpl implements WordExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final AdministrationPayService administrationPayService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    @Override
    public void exportToWord(OutputStream outputStream, ExportRequest request) throws IOException, NotFoundException {
        try (XWPFDocument document = new XWPFDocument()) {
            String dateFrom = request.getDateFrom().toString();
            String dateTo = request.getDateTo().toString();
            String mainIin = request.getIin();
            List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
            List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();

            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(mainIin);

            RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                    mainIin, dateFrom, dateTo);
            if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                iinsToProcess.addAll(
                        primaryRelations.getTypeToRelation().values().stream()
                                .flatMap(List::stream)
                                .map(RelationActive::getIin)
                                .filter(x -> x!= null && !x.isEmpty())
                                .distinct()
                                .toList()
                );
            }

            for (int i = 0; i < iinsToProcess.size(); i++) {
                String iin = iinsToProcess.get(i);

                addBoldParagraph(document, "Данные для ИИН: " + iin);
                document.createParagraph();

                Person person = portretService.getPerson(iin);
                RelationActiveWithTypes personPrimaryRelations = relationService.getPrimaryRelationsOfPerson(
                        iin, dateFrom, dateTo);
                RelationActiveWithTypes personSecondaryRelations = relationService.getSecondaryRelationsOfPerson(
                        iin, dateFrom, dateTo);
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

                // Add sections for this IIN
                addPortraitSection(document, person);
                addRelationsSection(document, personPrimaryRelations, personSecondaryRelations);
                addActivesAndIncomesSection(document, activeResponse, incomeResponse);
                addJobInformationSection(document, pensions, head, industry, turnoverRecords);

                if (i < iinsToProcess.size() - 1) {
                    XWPFParagraph pageBreak = document.createParagraph();
                    XWPFRun run = pageBreak.createRun();
                    run.addBreak(BreakType.PAGE);
                }
            }

            // Write to output stream
            document.write(outputStream);
        }
    }

    private void addPortraitSection(XWPFDocument document, Person person) {
        addBoldParagraph(document, "Портрет");

        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");
        XWPFTableRow row = table.getRow(0);

        // Left cell (image)
        XWPFTableCell imageCell = row.getCell(0);
        if (person.getImage() != null && !person.getImage().isEmpty()) {
            try {
                String imageString = person.getImage();
                byte[] imageBytes;
                int pictureType = XWPFDocument.PICTURE_TYPE_JPEG; // Default to JPEG
                String extension = "person_image.jpg";

                if (imageString.contains(";")) {
                    String[] imageParts = imageString.split(";");
                    String mimeType = imageParts[0].replace("data:", "");
                    String base64Data = imageParts[1].replace("base64,", "");
                    imageBytes = Base64.getDecoder().decode(base64Data);

                    if (mimeType.contains("png")) {
                        pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                        extension = "person_image.png";
                    } else if (!mimeType.contains("jpeg") && !mimeType.contains("jpg")) {
                        throw new IllegalArgumentException("Unsupported image format: " + mimeType);
                    }
                } else {
                    imageBytes = Base64.getDecoder().decode(imageString);
                }

                XWPFParagraph imagePara = imageCell.addParagraph();
                XWPFRun imageRun = imagePara.createRun();
                try (InputStream imageStream = new ByteArrayInputStream(imageBytes)) {
                    // Convert 100 pixels to EMUs (1 pixel ≈ 9525 EMUs)
                    int width = 100 * 9525;
                    int height = 100 * 9525;
                    imageRun.addPicture(imageStream, pictureType, extension, width, height);
                }
            } catch (Exception e) {
                log.error("Failed to add image to Word document: ", e);
                imageCell.setText("");
            }
        } else {
            imageCell.setText("");
        }

        // Right cell (person info)
        XWPFTableCell infoCell = row.getCell(1);
        XWPFParagraph infoPara = infoCell.addParagraph();
        addRun(infoPara, "ФИО: " + (person.getFio() != null ? person.getFio() : "-"));
        addRun(infoPara, "Возраст: " + (person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен"));
        addRun(infoPara, "ИИН: " + (person.getIin() != null ? person.getIin() : "-"));
        addRun(infoPara, "Портрет: " + (person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует"));
        addRun(infoPara, "Номинал: " + (person.getIsNominal() != null && person.getIsNominal() ? "Номинал" : "Не номинал"));
        addRun(infoPara, "Крипта: " + (person.getIsCryptoActive() ? "Есть переводы по криптовалюте" : "Нету переводов по криптовалюте"));

        document.createParagraph();
    }

    private void addRelationsSection(XWPFDocument document, RelationActiveWithTypes primaryRelations,
                                     RelationActiveWithTypes secondaryRelations) {
        // Primary Relations
        addBoldParagraph(document, "Первичные связи:");
        if (primaryRelations != null && primaryRelations.getTypeToRelation() != null && !primaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, primaryRelations.getTypeToRelation());
        } else {
            addParagraph(document, "Нет первичных связей");
        }
        document.createParagraph();

        // Secondary Relations
        addBoldParagraph(document, "Вторичные связи:");
        if (secondaryRelations != null && secondaryRelations.getTypeToRelation() != null && !secondaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, secondaryRelations.getTypeToRelation());
        } else {
            addParagraph(document, "Нет вторичных связей");
        }
        document.createParagraph();
    }

    private void addRelationsTable(XWPFDocument document, Map<String, List<RelationActive>> relationsMap) {
        log.debug("Adding relations table");

        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            addBoldParagraph(document, category + ":");

            if (relations == null || relations.isEmpty()) {
                addParagraph(document, "  Нет связей в данной категории");
                document.createParagraph();
                log.debug("No relations in category: {}", category);
                continue;
            }

            XWPFTable table = document.createTable(relations.size() + 1, 7);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ФИО", true);
            setTableCell(headerRow.getCell(1), "Связь", true);
            setTableCell(headerRow.getCell(2), "ИИН", true);
            setTableCell(headerRow.getCell(3), "Активы", true);
            setTableCell(headerRow.getCell(4), "Доходы", true);
            setTableCell(headerRow.getCell(5), "Номинал", true);
            setTableCell(headerRow.getCell(6), "Доп Инфо", true);

            int rowIndex = 1;
            for (RelationActive ra : relations) {
                log.debug("Processing RelationActive for IIN: {}, dopinfo: {}", ra.getIin(), ra.getDopinfo());
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), ra.getFio() != null ? ra.getFio() : "-", false);
                setTableCell(row.getCell(1), ra.getRelation() != null ? ra.getRelation() : "-", false);
                setTableCell(row.getCell(2), ra.getIin() != null ? ra.getIin() : "-", false);
                setTableCell(row.getCell(3), ra.getActives() != null ? ra.getActives() : "-", false);
                setTableCell(row.getCell(4), ra.getIncomes() != null ? ra.getIncomes() : "-", false);
                setTableCell(row.getCell(5), ra.isNominal() ? "Да" : "Нет", false);

                // Format dopinfo as a readable string
                String dopinfoStr = "-";
                if (ra.getDopinfo() != null && !ra.getDopinfo().isEmpty()) {
                    dopinfoStr = ra.getDopinfo().entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("; "));
                }
                setTableCell(row.getCell(6), dopinfoStr, false);
            }
            log.debug("Added relations table for category: {}", category);
            document.createParagraph();
        }
    }
    private void addActivesAndIncomesSection(XWPFDocument document, ActiveWithRecords activeResponse,
                                             IncomeWithRecords incomeResponse) {
        // Actives
        addBoldParagraph(document, "Активы:");
        if (activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()) {
           addActivesTable(document, activeResponse.getRecordsByOper());
        } else {
            addParagraph(document, "Нет данных об активах");
        }
        document.createParagraph();

        // Incomes
        addBoldParagraph(document, "Доходы:");
        if (incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()) {
            addIncomesTable(document, incomeResponse.getRecordsByYear());
        } else {
            addParagraph(document, "Нет данных о доходах");
        }
    }

    private void addActivesTable(XWPFDocument document, List<RecordDt> recordsByOper) {
        addBoldParagraph(document, "Активы:");

        if (recordsByOper == null || recordsByOper.isEmpty()) {
            addParagraph(document, "  Нет записей для активов");
            document.createParagraph();
            return;
        }

        boolean hasESFRecords = recordsByOper.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

        XWPFTable table;
        if (hasESFRecords) {
            table = document.createTable(recordsByOper.size() + 1, 9);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
            setTableCell(headerRow.getCell(1), "ИИН Покуп.", true);
            setTableCell(headerRow.getCell(2), "ИИН Прод.", true);
            setTableCell(headerRow.getCell(3), "Дата", true);
            setTableCell(headerRow.getCell(4), "База данных", true);
            setTableCell(headerRow.getCell(5), "Активы", true);
            setTableCell(headerRow.getCell(6), "Операция", true);
            setTableCell(headerRow.getCell(7), "Доп. инфо", true);
            setTableCell(headerRow.getCell(8), "Сумма", true);

            int rowIndex = 1;
            for (RecordDt record : recordsByOper) {
                XWPFTableRow row = table.getRow(rowIndex++);
                if (record instanceof ESFInformationRecordDt esfRecord) {
                    setTableCell(row.getCell(0), esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "-", false);
                    setTableCell(row.getCell(1), esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "-", false);
                    setTableCell(row.getCell(2), esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "-", false);
                    setTableCell(row.getCell(3), esfRecord.getDate() != null ? esfRecord.getDate().toString() : "-", false);
                    setTableCell(row.getCell(4), esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "-", false);
                    setTableCell(row.getCell(5), esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "-", false);
                    setTableCell(row.getCell(6), esfRecord.getOper() != null ? esfRecord.getOper() : "-", false);
                    setTableCell(row.getCell(7), esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "-", false);
                    setTableCell(row.getCell(8), esfRecord.getSumm() != null ? esfRecord.getSumm() : "-", false);
                } else if(record instanceof InformationRecordDt info){
                    setTableCell(row.getCell(0), info.getIin_bin() != null ? record.getIin_bin() : "-", false);
                    setTableCell(row.getCell(1), "-", false);
                    setTableCell(row.getCell(2), "-", false);
                    setTableCell(row.getCell(3), info.getDate() != null ? record.getDate().toString() : "-", false);
                    setTableCell(row.getCell(4), info.getDatabase() != null ? record.getDatabase() : "-", false);
                    setTableCell(row.getCell(5), "-", false);
                    setTableCell(row.getCell(6), info.getOper() != null ? record.getOper() : "-", false);
                    setTableCell(row.getCell(7), info.getDopinfo() != null ? record.getDopinfo() : "-", false);
                    setTableCell(row.getCell(8), info.getSumm() != null ? record.getSumm() : "-", false);
                } else if(record instanceof NaoConRecordDt nao){
                    setTableCell(row.getCell(0), nao.getIin_bin() != null ? record.getIin_bin() : "-", false);
                    setTableCell(row.getCell(1), "-", false);
                    setTableCell(row.getCell(2), "-", false);
                    setTableCell(row.getCell(3), nao.getDate() != null ? record.getDate().toString() : "-", false);
                    setTableCell(row.getCell(4), nao.getDatabase() != null ? record.getDatabase() : "-", false);
                    setTableCell(row.getCell(5), "-", false);
                    setTableCell(row.getCell(6), nao.getOper() != null ? record.getOper() : "-", false);
                    setTableCell(row.getCell(7), nao.getDopinfo() != null ? record.getDopinfo() : "-", false);
                    setTableCell(row.getCell(8), nao.getSumm() != null ? record.getSumm() : "-", false);
                }
            }
        } else {
            table = document.createTable(recordsByOper.size() + 1, 6);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
            setTableCell(headerRow.getCell(1), "Дата", true);
            setTableCell(headerRow.getCell(2), "База данных", true);
            setTableCell(headerRow.getCell(3), "Операция", true);
            setTableCell(headerRow.getCell(4), "Доп. инфо", true);
            setTableCell(headerRow.getCell(5), "Сумма", true);

            int rowIndex = 1;
            for (RecordDt record : recordsByOper) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), record.getIin_bin() != null ? record.getIin_bin() : "-", false);
                setTableCell(row.getCell(1), record.getDate() != null ? record.getDate().toString() : "-", false);
                setTableCell(row.getCell(2), record.getDatabase() != null ? record.getDatabase() : "-", false);
                setTableCell(row.getCell(3), record.getOper() != null ? record.getOper() : "-", false);
                setTableCell(row.getCell(4), record.getDopinfo() != null ? record.getDopinfo() : "-", false);
                setTableCell(row.getCell(5), record.getSumm() != null ? record.getSumm() : "-", false);
            }
        }

        document.createParagraph();
    }

    private void addIncomesTable(XWPFDocument document, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            addParagraph(document, "  Нет записей о доходах");
            return;
        }

        XWPFTable table = document.createTable(records.size() + 1, 6);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
        setTableCell(headerRow.getCell(1), "Дата", true);
        setTableCell(headerRow.getCell(2), "База данных", true);
        setTableCell(headerRow.getCell(3), "Операция", true);
        setTableCell(headerRow.getCell(4), "Доп. инфо", true);
        setTableCell(headerRow.getCell(5), "Сумма", true);

        int rowIndex = 1;
        for (RecordDt record : records) {
            XWPFTableRow row = table.getRow(rowIndex++);
            setTableCell(row.getCell(0), record.getIin_bin() != null ? record.getIin_bin() : "-", false);
            setTableCell(row.getCell(1), record.getDate() != null ? record.getDate().toString() : "-", false);
            setTableCell(row.getCell(2), record.getDatabase() != null ? record.getDatabase() : "-", false);
            setTableCell(row.getCell(3), record.getOper() != null ? record.getOper() : "-", false);
            setTableCell(row.getCell(4), record.getDopinfo() != null ? record.getDopinfo() : "-", false);
            setTableCell(row.getCell(5), record.getSumm() != null ? record.getSumm() : "-", false);
        }
    }

    private void addJobInformationSection(XWPFDocument document, List<Pension> pensions,
                                          Head head, Industry industry,
                                          List<TurnoverRecord> turnoverRecords) {
        // Industry section
        addBoldParagraph(document, "Отрасль:");
        addParagraph(document, industry != null && industry.getName() != null && !industry.getName().isEmpty()
                ? industry.getName()
                : "Информация об отрасли отсутствует");
        document.createParagraph();

        // Pensions section
        addBoldParagraph(document, "Пенсионные взносы:");
        if (pensions != null && !pensions.isEmpty()) {
            addPensionsTable(document, pensions);
        } else {
            addParagraph(document, "Нет данных о пенсионных взносах");
        }


        // Head section
        addBoldParagraph(document, "Руководящие позиции:");
        if (!head.isEmpty()) {
            addHeadInformationTable(document, head);
        } else {
            addParagraph(document, "Нет информации о руководящих позициях");
        }
        document.createParagraph();

        // Банковские счета
        addBoldParagraph(document, "Банковские счета:");
        if (!turnoverRecords.isEmpty()) {
            addTurnoversTable(document, turnoverRecords);
        } else {
            addParagraph(document, "Нет информации о банковских счетах");
        }
        document.createParagraph();

    }

    private void addHeadInformationTable(XWPFDocument document, Head head) {
        // Supervisor information
        if (head.getHead() != null && !head.getHead().isEmpty()) {
            addBoldParagraph(document, "Руководящие должности:");
            XWPFTable table = document.createTable(head.getHead().size() + 1, 5);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
            setTableCell(headerRow.getCell(1), "Тип позиции", true);
            setTableCell(headerRow.getCell(2), "ИИН/БИН налогоплательщика", true);
            setTableCell(headerRow.getCell(3), "Тип", true);
            setTableCell(headerRow.getCell(4), "Наименование", true);

            int rowIndex = 1;
            for (SupervisorRecord supervisor : head.getHead()) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "-", false);
                setTableCell(row.getCell(1), supervisor.getPositionType() != null ? supervisor.getPositionType() : "-", false);
                setTableCell(row.getCell(2), supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "-", false);
               // setTableCell(row.getCell(3), supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "-", false);
                setTableCell(row.getCell(4), supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "-", false);
            }
        }

        // Financial summary
        addBoldParagraph(document, "Финансовая информация:");
        addParagraph(document, "Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "-"));
        addParagraph(document, "Налоги: " + (head.getTax() != null ? head.getTax().toString() : "-"));

        // ESF information
        if (head.getEsf() != null && !head.getEsf().isEmpty()) {
            addBoldParagraph(document, "ESF информация:");
            XWPFTable table = document.createTable(head.getEsf().size() + 1, 4);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
            setTableCell(headerRow.getCell(1), "Дата", true);
            setTableCell(headerRow.getCell(2), "Активы", true);
            setTableCell(headerRow.getCell(3), "Сумма", true);

            int rowIndex = 1;
            for (org.info.infobaza.model.info.active_income.EsfOverall esf : head.getEsf()) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), esf.getIin_bin() != null ? esf.getIin_bin() : "-", false);
                setTableCell(row.getCell(1), esf.getDate() != null ? esf.getDate().toString() : "-", false);
                setTableCell(row.getCell(2), esf.getAktivy() != null ? esf.getAktivy() : "-", false);
                setTableCell(row.getCell(3), esf.getSumm() != null ? esf.getSumm().toString() : "-", false);
            }
        }

        // Statuses
        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            addBoldParagraph(document, "Статусы:");
            addParagraph(document, String.join(", ", head.getStatuses()));
        }
    }

    private void addPensionsTable(XWPFDocument document, List<Pension> pensions) {
        XWPFTable table = document.createTable(pensions.size() + 1, 7);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setTableCell(headerRow.getCell(0), "Дата с", true);
        setTableCell(headerRow.getCell(1), "Дата по", true);
        setTableCell(headerRow.getCell(2), "Наименование", true);
        setTableCell(headerRow.getCell(3), "P_RNN", true);
        setTableCell(headerRow.getCell(4), "Максимальная з.п.", true);
        setTableCell(headerRow.getCell(5), "Последняя з.п.", true);
        setTableCell(headerRow.getCell(6), "Суммарно", true);

        int rowIndex = 1;
        for (Pension pension : pensions) {
            XWPFTableRow row = table.getRow(rowIndex++);
            setTableCell(row.getCell(0), pension.getDateFrom() != null ? pension.getDateFrom() : "-", false);
            setTableCell(row.getCell(1), pension.getDateTo() != null ? pension.getDateTo() : "-", false);
            setTableCell(row.getCell(2), pension.getName() != null ? pension.getName() : "-", false);
            setTableCell(row.getCell(3), pension.getP_RNN() != null ? pension.getP_RNN() : "-", false);
            setTableCell(row.getCell(4), pension.getMaxSalary() != null ? pension.getMaxSalary() : "-", false);
            setTableCell(row.getCell(5), pension.getLastSalary() != null ? pension.getLastSalary() : "-", false);
            setTableCell(row.getCell(6), pension.getSumm() != null ? pension.getSumm() : "-", false);
        }
    }

    private void addTurnoversTable(XWPFDocument document, List<TurnoverRecord> turnoverRecords){
        XWPFTable table = document.createTable(turnoverRecords.size() + 1, 7);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setTableCell(headerRow.getCell(0), "ИИН/БИН", true);
        setTableCell(headerRow.getCell(1), "Название банка", true);
        setTableCell(headerRow.getCell(2), "Счет", true);
        setTableCell(headerRow.getCell(3), "Сумма", true);
        setTableCell(headerRow.getCell(4), "Дата от", true);
        setTableCell(headerRow.getCell(5), "Дата до", true);
        setTableCell(headerRow.getCell(6), "Источник", true);

        int rowIndex = 1;
        for (TurnoverRecord turnoverRecord : turnoverRecords) {
            XWPFTableRow row = table.getRow(rowIndex++);
            setTableCell(row.getCell(0), turnoverRecord.getIinBin() != null ? turnoverRecord.getIinBin() : "-", false);
            setTableCell(row.getCell(1), turnoverRecord.getBankName() != null ? turnoverRecord.getBankName() : "-", false);
            setTableCell(row.getCell(2), turnoverRecord.getBankAccount() != null ? turnoverRecord.getBankAccount() : "-", false);
            setTableCell(row.getCell(3), turnoverRecord.getSumm() != null ? turnoverRecord.getSumm() : "-", false);
            setTableCell(row.getCell(4), turnoverRecord.getStartDate() != null ? turnoverRecord.getStartDate() : "-", false);
            setTableCell(row.getCell(5), turnoverRecord.getEndDate() != null ? turnoverRecord.getEndDate() : "-", false);
            setTableCell(row.getCell(6), turnoverRecord.getSource() != null ? turnoverRecord.getSource() : "-", false);
        }
    }
    private void addBoldParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(12);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(12);
    }

    private void addRun(XWPFParagraph paragraph, String text) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.addBreak();
    }

    private void setTableCell(XWPFTableCell cell, String text, boolean isBold) {
        XWPFParagraph para = cell.getParagraphs().get(0);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(isBold);
        run.setFontSize(12);
    }
}