package org.info.infobaza.service.export.impl;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.dto.response.info.active.ActiveWithRecords;
import org.info.infobaza.dto.response.info.income.IncomeWithRecords;
import org.info.infobaza.dto.response.job.Head;
import org.info.infobaza.dto.response.job.Industry;
import org.info.infobaza.dto.response.job.Pension;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.dto.response.relation.RelationActiveWithTypes;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.service.Analyzer;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    @Override
    public void exportToWord(OutputStream outputStream, ExportRequest request) throws IOException, NotFoundException {
        XWPFDocument document = new XWPFDocument();
        try {
            String dateFrom = request.getDateFrom().toString();
            String dateTo = request.getDateTo().toString();
            String iin = request.getIin();
            List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
            List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();


            // Fetch Person data
            Person person = portretService.getPerson(iin);

            // Fetch Relations data
            RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                    iin, dateFrom, dateTo);

            RelationActiveWithTypes secondaryRelations = relationService.getSecondaryRelationsOfPerson(
                    iin, dateFrom, dateTo);

            // Fetch job information
            List<Pension> pensions = enpfService.getPension(
                    iin,
                    dateFrom, dateTo);

            Head head = headService.constructHead(
                    iin,
                    dateFrom, dateTo);

            Industry industry = industrialService.getIndustry(iin);

            // Fetch Active and Income data
            ActiveWithRecords activeResponse = (ActiveWithRecords) analyzer.getAllActivesOfPersonsByDates(
                    iin,
                    dateFrom, dateTo,
                    yearsActive,
                    request.getVids(),
                    request.getTypes(),
                    request.getSources(),
                    request.getIins());

            IncomeWithRecords incomeResponse = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                    iin,
                    dateFrom, dateTo,
                    yearsIncome,
                    request.getVids(),
                    request.getSources(),
                    request.getIins());

            addPortraitSection(document, person);
            addRelationsSection(document, primaryRelations, secondaryRelations);
            addActivesAndIncomesSection(document, activeResponse, incomeResponse);
            addJobInformationSection(document, pensions, head, industry);

            // Write to output stream
            document.write(outputStream);
        } catch (NotFoundException e) {
            log.error("Data not found: ", e);
            throw e;
        } finally {
            document.close();
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
        addRun(infoPara, "ФИО: " + (person.getFio() != null ? person.getFio() : "N/A"));
        addRun(infoPara, "Возраст: " + (person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен"));
        addRun(infoPara, "ИИН: " + (person.getIin() != null ? person.getIin() : "N/A"));
        addRun(infoPara, "Портрет: " + (person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует"));
        addRun(infoPara, "Номинал: " + (person.getIsNominal() != null && person.getIsNominal() ? "Номинал" : "Не номинал"));

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
        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            addBoldParagraph(document, category + ":");

            if (relations == null || relations.isEmpty()) {
                addParagraph(document, "  Нет связей в данной категории");
                document.createParagraph();
                continue;
            }

            XWPFTable table = document.createTable(relations.size() + 1, 6);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "ФИО", true);
            setTableCell(headerRow.getCell(1), "Связь", true);
            setTableCell(headerRow.getCell(2), "ИИН", true);
            setTableCell(headerRow.getCell(3), "Активы", true);
            setTableCell(headerRow.getCell(4), "Доходы", true);
            setTableCell(headerRow.getCell(5), "Номинал", true);

            int rowIndex = 1;
            for (RelationActive ra : relations) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), ra.getFio() != null ? ra.getFio() : "N/A", false);
                setTableCell(row.getCell(1), ra.getRelation() != null ? ra.getRelation() : "N/A", false);
                setTableCell(row.getCell(2), ra.getIin() != null ? ra.getIin() : "N/A", false);
                setTableCell(row.getCell(3), ra.getActives() != null ? ra.getActives() : "N/A", false);
                setTableCell(row.getCell(4), ra.getIncomes() != null ? ra.getIncomes() : "N/A", false);
                setTableCell(row.getCell(5), ra.isNominal() ? "Да": "Нет", false);
            }
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

    private void addActivesTable(XWPFDocument document, Map<String, List<RecordDt>> recordsByOper) {
        for (Map.Entry<String, List<RecordDt>> entry : recordsByOper.entrySet()) {
            String operation = entry.getKey();
            List<RecordDt> records = entry.getValue();

            addBoldParagraph(document, operation + ":");

            if (records == null || records.isEmpty()) {
                addParagraph(document, "  Нет записей для данной операции");
                document.createParagraph();
                continue;
            }

            boolean hasESFRecords = records.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

            XWPFTable table;
            if (hasESFRecords) {
                table = document.createTable(records.size() + 1, 9);
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
                for (RecordDt record : records) {
                    XWPFTableRow row = table.getRow(rowIndex++);
                    if (record instanceof ESFInformationRecordDt esfRecord) {
                        setTableCell(row.getCell(0), esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "N/A", false);
                        setTableCell(row.getCell(1), esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "N/A", false);
                        setTableCell(row.getCell(2), esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "N/A", false);
                        setTableCell(row.getCell(3), esfRecord.getDate() != null ? esfRecord.getDate().toString() : "N/A", false);
                        setTableCell(row.getCell(4), esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "N/A", false);
                        setTableCell(row.getCell(5), esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "N/A", false);
                        setTableCell(row.getCell(6), esfRecord.getOper() != null ? esfRecord.getOper() : "N/A", false);
                        setTableCell(row.getCell(7), esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "N/A", false);
                        setTableCell(row.getCell(8), esfRecord.getSumm() != null ? esfRecord.getSumm() : "N/A", false);
                    } else {
                        setTableCell(row.getCell(0), record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
                        setTableCell(row.getCell(1), "N/A", false);
                        setTableCell(row.getCell(2), "N/A", false);
                        setTableCell(row.getCell(3), record.getDate() != null ? record.getDate().toString() : "N/A", false);
                        setTableCell(row.getCell(4), record.getDatabase() != null ? record.getDatabase() : "N/A", false);
                        setTableCell(row.getCell(5), "N/A", false);
                        setTableCell(row.getCell(6), record.getOper() != null ? record.getOper() : "N/A", false);
                        setTableCell(row.getCell(7), record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
                        setTableCell(row.getCell(8), record.getSumm() != null ? record.getSumm() : "N/A", false);
                    }
                }
            } else {
                table = document.createTable(records.size() + 1, 6);
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
                    setTableCell(row.getCell(0), record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
                    setTableCell(row.getCell(1), record.getDate() != null ? record.getDate().toString() : "N/A", false);
                    setTableCell(row.getCell(2), record.getDatabase() != null ? record.getDatabase() : "N/A", false);
                    setTableCell(row.getCell(3), record.getOper() != null ? record.getOper() : "N/A", false);
                    setTableCell(row.getCell(4), record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
                    setTableCell(row.getCell(5), record.getSumm() != null ? record.getSumm() : "N/A", false);
                }
            }
        }
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
            setTableCell(row.getCell(0), record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
            setTableCell(row.getCell(1), record.getDate() != null ? record.getDate().toString() : "N/A", false);
            setTableCell(row.getCell(2), record.getDatabase() != null ? record.getDatabase() : "N/A", false);
            setTableCell(row.getCell(3), record.getOper() != null ? record.getOper() : "N/A", false);
            setTableCell(row.getCell(4), record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
            setTableCell(row.getCell(5), record.getSumm() != null ? record.getSumm() : "N/A", false);
        }
    }

    private void addJobInformationSection(XWPFDocument document, List<Pension> pensions, Head head, Industry industry) {
        // Industry section
        addBoldParagraph(document, "Отрасль:");
        addParagraph(document, industry != null && industry.getName() != null && !industry.getName().isEmpty()
                ? industry.getName()
                : "Информация об отрасли отсутствует");
        document.createParagraph();

        // Head section
        addBoldParagraph(document, "Руководящие позиции:");
        if (!head.isEmpty()) {
            addHeadInformationTable(document, head);
        } else {
            addParagraph(document, "Нет информации о руководящих позициях");
        }
        document.createParagraph();

        // Pensions section
        addBoldParagraph(document, "Пенсионные взносы:");
        if (pensions != null && !pensions.isEmpty()) {
            addPensionsTable(document, pensions);
        } else {
            addParagraph(document, "Нет данных о пенсионных взносах");
        }
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
                setTableCell(row.getCell(0), supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "N/A", false);
                setTableCell(row.getCell(1), supervisor.getPositionType() != null ? supervisor.getPositionType() : "N/A", false);
                setTableCell(row.getCell(2), supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "N/A", false);
                setTableCell(row.getCell(3), supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "N/A", false);
                setTableCell(row.getCell(4), supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "N/A", false);
            }
        }

        if (head.getOked() != null && !head.getOked().isEmpty()) {
            addBoldParagraph(document, "Компании:");
            XWPFTable table = document.createTable(head.getOked().size() + 1, 5);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            setTableCell(headerRow.getCell(0), "Русское название", true);
            setTableCell(headerRow.getCell(1), "Оригинальное название", true);
            setTableCell(headerRow.getCell(2), "БИН", true);
            setTableCell(headerRow.getCell(3), "Дата регистрации", true);
            setTableCell(headerRow.getCell(4), "Телефон", true);

            int rowIndex = 1;
            for (CompanyRecord company : head.getOked()) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setTableCell(row.getCell(0), company.getRusName() != null ? company.getRusName() : "N/A", false);
                setTableCell(row.getCell(1), company.getOrigName() != null ? company.getOrigName() : "N/A", false);
                setTableCell(row.getCell(2), company.getBin() != null ? company.getBin() : "N/A", false);
                setTableCell(row.getCell(3), company.getDateReg() != null ? company.getDateReg().toString() : "N/A", false);
                setTableCell(row.getCell(4), company.getTelephone() != null ? company.getTelephone() : "N/A", false);
            }
        }

        // Financial summary
        addBoldParagraph(document, "Финансовая информация:");
        addParagraph(document, "Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "N/A"));
        addParagraph(document, "Налоги: " + (head.getTax() != null ? head.getTax().toString() : "N/A"));

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
                setTableCell(row.getCell(0), esf.getIin_bin() != null ? esf.getIin_bin() : "N/A", false);
                setTableCell(row.getCell(1), esf.getDate() != null ? esf.getDate().toString() : "N/A", false);
                setTableCell(row.getCell(2), esf.getAktivy() != null ? esf.getAktivy() : "N/A", false);
                setTableCell(row.getCell(3), esf.getSumm() != null ? esf.getSumm().toString() : "N/A", false);
            }
        }

        // Statuses
        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            addBoldParagraph(document, "Статусы:");
            addParagraph(document, String.join(", ", head.getStatuses()));
        }
    }

    private void addPensionsTable(XWPFDocument document, List<Pension> pensions) {
        XWPFTable table = document.createTable(pensions.size() + 1, 4);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setTableCell(headerRow.getCell(0), "Дата с", true);
        setTableCell(headerRow.getCell(1), "Дата по", true);
        setTableCell(headerRow.getCell(2), "Наименование", true);
        setTableCell(headerRow.getCell(3), "P_RNN", true);

        int rowIndex = 1;
        for (Pension pension : pensions) {
            XWPFTableRow row = table.getRow(rowIndex++);
            setTableCell(row.getCell(0), pension.getDateFrom() != null ? pension.getDateFrom() : "N/A", false);
            setTableCell(row.getCell(1), pension.getDateTo() != null ? pension.getDateTo() : "N/A", false);
            setTableCell(row.getCell(2), pension.getName() != null ? pension.getName() : "N/A", false);
            setTableCell(row.getCell(3), pension.getP_RNN() != null ? pension.getP_RNN() : "N/A", false);
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