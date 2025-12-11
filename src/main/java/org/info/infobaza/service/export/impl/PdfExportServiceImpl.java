package org.info.infobaza.service.export.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.adm_shtraf.AdministrationPayService;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.enpf.IndustrialService;
import org.info.infobaza.service.export.PdfExportService;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.service.relations.RelationService;
import org.info.infobaza.util.date.DateUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.info.infobaza.constants.Dictionary.RU;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final AdministrationPayService administrationPayService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    @Override
    public void exportToPdf(OutputStream outputStream, ExportRequest request) throws DocumentException, IOException {
        log.info("Starting PDF export for request: IIN={}, dateFrom={}, dateTo={}",
                request.getIin(), request.getDateFrom(), request.getDateTo());

        String dateFrom = request.getDateFrom() != null ? request.getDateFrom().toString() : null;
        String dateTo = request.getDateTo() != null ? request.getDateTo().toString() : null;
        String mainIin = request.getIin();
        List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
        List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();

        // Validate input
        if (mainIin == null || mainIin.isEmpty()) {
            log.error("Invalid request: mainIin is null or empty");
            throw new IllegalArgumentException("Main IIN cannot be null or empty");
        }
        if (dateFrom == null || dateTo == null) {
            log.error("Invalid request: dateFrom or dateTo is null");
            throw new IllegalArgumentException("Date range cannot be null");
        }

        // Create PDF document
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        log.debug("PDF document created and PdfWriter initialized");

        // === ИНИЦИАЛИЗАЦИЯ ШРИФТОВ С РАЗНЫМИ РАЗМЕРАМИ ===
        BaseFont baseFont;
        Font titleFont, headerFont, subHeaderFont, bodyFont, tableFont, smallFont;
        Font font;      // для совместимости
        Font boldFont;  // для совместимости

        try {
            baseFont = getBaseFont();

            // Новые стили шрифтов
            titleFont     = new Font(baseFont, 16, Font.BOLD);   // "Данные для ИИН"
            headerFont    = new Font(baseFont, 13, Font.BOLD);   // "Активы:", "Доходы:"
            subHeaderFont = new Font(baseFont, 11, Font.BOLD);   // заголовки таблиц
            bodyFont      = new Font(baseFont, 11);              // основной текст
            tableFont     = new Font(baseFont, 9);               // текст в таблицах
            smallFont     = new Font(baseFont, 8);               // примечания

            // Совместимость со старым кодом

            log.debug("Custom fonts with sizes initialized: title=16, header=13, body=11, table=9");
        } catch (Exception e) {
            log.error("Failed to load custom font, using Helvetica", e);

            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            titleFont     = new Font(baseFont, 16, Font.BOLD);
            headerFont    = new Font(baseFont, 13, Font.BOLD);
            subHeaderFont = new Font(baseFont, 11, Font.BOLD);
            bodyFont      = new Font(baseFont, 11);
            tableFont     = new Font(baseFont, 9);

        }

        // Set watermark
        try {
            Font watermarkFont = new Font(baseFont, 12);
            writer.setPageEvent(new WatermarkPageEvent(watermarkFont));
            log.debug("Watermark set successfully");
        } catch (Exception e) {
            log.warn("Could not set up watermark: {}", e.getMessage());
        }

        document.open();
        log.debug("PDF document opened");

        try {
            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(mainIin);

            // Fetch primary relations
            try {
                RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(mainIin, dateFrom, dateTo);
                if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                    List<String> relatedIins = primaryRelations.getTypeToRelation().values().stream()
                            .flatMap(List::stream)
                            .filter(relation -> relation.getLevel() == 1)
                            .map(RelationActive::getIin)
                            .filter(x -> x != null && !x.isEmpty())
                            .distinct()
                            .toList();
                    iinsToProcess.addAll(relatedIins);
                    log.info("Added {} related IINs from primary relations", relatedIins.size());
                } else {
                    log.warn("No primary relations found for IIN: {}", mainIin);
                }
            } catch (Exception e) {
                log.error("Error fetching primary relations for IIN {}: {}", mainIin, e.getMessage());
            }

            log.info("Processing {} IINs: {}", iinsToProcess.size(), iinsToProcess);

            boolean contentAdded = false;
            for (String iin : iinsToProcess) {
                log.info("Processing IIN: {}", iin);
                try {
                    // Add IIN header
                    document.add(new Paragraph("Данные для ИИН: " + iin, titleFont));
                    document.add(new Paragraph(" ", bodyFont));
                    contentAdded = true;
                    log.debug("Added header for IIN: {}", iin);

                    // Fetch Person data
                    Person person = null;
                    person = portretService.getPerson(iin);
                    log.debug("Fetched person data for IIN: {}", iin);

                    // Fetch Relations data
                    RelationActiveWithTypes personPrimaryRelations = null;
                    RelationActiveWithTypes personSecondaryRelations = null;
                    try {
                        personPrimaryRelations = relationService.getPrimaryRelationsOfPerson(iin, dateFrom, dateTo);
                        personSecondaryRelations = relationService.getSecondaryRelationsOfPerson(iin, dateFrom, dateTo);
                    } catch (Exception e) {
                        log.error("Error fetching relations for IIN {}: {}", iin, e.getMessage());
                        document.add(new Paragraph("Error fetching relations for IIN: " + iin, bodyFont));
                        contentAdded = true;
                    }

                    // Fetch job information
                    List<Pension> pensions = null;
                    Head head = null;
                    Industry industry = null;
                    List<TurnoverRecord> turnoverRecords = null;
                    try {
                        pensions = enpfService.getPension(iin, dateFrom, dateTo);
                        head = headService.constructHead(iin, dateFrom, dateTo);
                        industry = industrialService.getIndustry(iin);
                        turnoverRecords = enpfService.getTurnoverRecords(iin);
                        log.debug("Fetched job information for IIN: {}", iin);
                    } catch (Exception e) {
                        log.error("Error fetching job information for IIN {}: {}", iin, e.getMessage());
                        document.add(new Paragraph("Error fetching job information for IIN: " + iin, bodyFont));
                        contentAdded = true;
                    }

                    // Fetch Active and Income data
                    ActiveWithRecords activeResponse = null;
                    IncomeWithRecords incomeResponse = null;
                    try {
                        Object activeObj = analyzer.getAllActivesOfPersonsByDates(
                                iin, dateFrom, dateTo, yearsActive, request.getVids(), request.getTypes(),
                                request.getSources(), request.getIins());
                        log.info("Active response type for IIN {}: {}", iin, activeObj != null ? activeObj.getClass().getName() : "null");
                        activeResponse = (ActiveWithRecords) activeObj;

                        Object incomeObj = analyzer.getAllIncomesOfPersonsByDates(
                                iin, dateFrom, dateTo, yearsIncome, request.getVids(), request.getSources(),
                                request.getIins());
                        log.info("Income response type for IIN {}: {}", iin, incomeObj != null ? incomeObj.getClass().getName() : "null");
                        incomeResponse = (IncomeWithRecords) incomeObj;
                        log.debug("Fetched active and income data for IIN: {}", iin);
                    } catch (ClassCastException e) {
                        log.error("Invalid response type from Analyzer for IIN {}: {}", iin, e.getMessage());
                        document.add(new Paragraph("Error fetching active/income data for IIN: " + iin, bodyFont));
                        contentAdded = true;
                    } catch (Exception e) {
                        log.error("Error fetching active/income data for IIN {}: {}", iin, e.getMessage());
                        document.add(new Paragraph("Error fetching active/income data for IIN: " + iin, bodyFont));
                        contentAdded = true;
                    }

                    // Add sections
                    if (person != null) {
                        addPortraitSection(document, person, bodyFont, tableFont);
                        log.debug("Added portrait section for IIN: {}", iin);
                    }
                    addRelationsSection(document, personPrimaryRelations, personSecondaryRelations, bodyFont, headerFont, subHeaderFont, tableFont);
                    addActivesAndIncomesSection(document, activeResponse, incomeResponse, bodyFont, headerFont, subHeaderFont, tableFont);
                    addJobInformationSection(document, pensions, head, industry, turnoverRecords, bodyFont, headerFont, subHeaderFont, tableFont);

                } catch (DocumentException e) {
                    log.error("DocumentException while processing IIN {}: {}", iin, e.getMessage());
                    document.add(new Paragraph("Error processing data for IIN: " + iin, bodyFont));
                    contentAdded = true;
                } catch (Exception e) {
                    log.error("Unexpected error processing IIN {}: {}", iin, e.getMessage());
                    document.add(new Paragraph("Unexpected error processing data for IIN: " + iin, bodyFont));
                    contentAdded = true;
                }
            }

            if (!contentAdded) {
                log.warn("No content added to PDF document for request: {}", request);
                document.add(new Paragraph("No data available for IIN: " + mainIin, bodyFont));
                log.info("Added fallback content for empty document");
            }

        } finally {
            document.close();
            log.debug("PDF document closed");
        }
        log.info("PDF export completed for IIN: {}", mainIin);
    }

    private void addPortraitSection(Document document, Person person, Font bodyFont, Font tableFont) throws DocumentException {
        log.debug("Adding portrait section for person: {}", person != null ? person.getIin() : "null");
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3});

        addImageCell(table, person);
        addPersonInfoCell(table, person, bodyFont);

        document.add(table);
        document.add(new Paragraph(" ", bodyFont));
    }

    private void addImageCell(PdfPTable table, Person person) {
        if (person != null && person.getImage() != null && !person.getImage().isEmpty()) {
            try {
                String imageData = person.getImage().replaceFirst("^data:image/[^;]+;base64,?", "");
                byte[] imageBytes = Base64.getDecoder().decode(imageData);
                Image image = Image.getInstance(imageBytes);
                image.scaleToFit(100, 100);

                PdfPCell imageCell = new PdfPCell(image);
                imageCell.setBorder(PdfPCell.NO_BORDER);
                imageCell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
                table.addCell(imageCell);
                log.debug("Added image cell for person: {}", person.getIin());
            } catch (Exception e) {
                log.error("Failed to add image to PDF for person {}: {}", person.getIin(), e.getMessage());
                addEmptyCell(table);
            }
        } else {
            log.debug("No image available for person: {}", person != null ? person.getIin() : "null");
            addEmptyCell(table);
        }
    }

    private void addPersonInfoCell(PdfPTable table, Person person, Font bodyFont) {
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(PdfPCell.NO_BORDER);

        if (person != null) {
            textCell.addElement(new Paragraph(person.getFio() != null ? person.getFio() : "-", bodyFont));
            textCell.addElement(new Paragraph((person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен"), bodyFont));
            textCell.addElement(new Paragraph(person.getIin() != null ? person.getIin() : "-", bodyFont));
            textCell.addElement(new Paragraph((person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует"), bodyFont));
            textCell.addElement(new Paragraph(((person.getIsNominal() != null && person.getIsNominal()) ? "Номинал" : "Не номинал"), bodyFont));
            textCell.addElement(new Paragraph(((person.getIsNominalUl() != null && person.getIsNominalUl()) ? "Подставной владелец" : "Не подставной владелец"), bodyFont));
            textCell.addElement(new Paragraph((person.getIsCryptoActive() ? "Есть переводы по криптовалюте" : "Нету переводов по криптовалюте"), bodyFont));
            log.debug("Added person info cell for IIN: {}", person.getIin());
        } else {
            textCell.addElement(new Paragraph("Person data not available", bodyFont));
            log.debug("Added fallback person info cell");
        }

        table.addCell(textCell);
    }

    private void addEmptyCell(PdfPTable table) {
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(emptyCell);
        log.debug("Added empty cell to table");
    }

    private void addRelationsSection(Document document, RelationActiveWithTypes primaryRelations,
                                     RelationActiveWithTypes secondaryRelations, Font bodyFont, Font headerFont,
                                     Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding relations section");

        document.add(new Paragraph("Первичные связи:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (primaryRelations != null && primaryRelations.getTypeToRelation() != null && !primaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, primaryRelations.getTypeToRelation(), bodyFont, subHeaderFont, tableFont);
            log.debug("Added primary relations table");
        } else {
            document.add(new Paragraph("Нет первичных связей", bodyFont));
            log.debug("No primary relations available");
        }

        document.add(new Paragraph(" ", bodyFont));

        document.add(new Paragraph("Вторичные связи:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (secondaryRelations != null && secondaryRelations.getTypeToRelation() != null && !secondaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, secondaryRelations.getTypeToRelation(), bodyFont, subHeaderFont, tableFont);
            log.debug("Added secondary relations table");
        } else {
            document.add(new Paragraph("Нет вторичных связей", bodyFont));
            log.debug("No secondary relations available");
        }

        document.add(new Paragraph(" ", bodyFont));
    }

    private void addActivesAndIncomesSection(Document document, ActiveWithRecords activeResponse,
                                             IncomeWithRecords incomeResponse, Font bodyFont, Font headerFont,
                                             Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding actives and incomes section");

        document.add(new Paragraph("Активы:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()) {
            addActivesTable(document, activeResponse.getRecordsByOper(), bodyFont, subHeaderFont, tableFont);
            log.debug("Added actives table");
        } else {
            document.add(new Paragraph("Нет данных об активах", bodyFont));
            log.debug("No actives data available");
        }

        document.add(new Paragraph(" ", bodyFont));

        document.add(new Paragraph("Доходы:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()) {
            addIncomesTable(document, incomeResponse.getRecordsByYear(), bodyFont, subHeaderFont, tableFont);
            log.debug("Added incomes table");
        } else {
            document.add(new Paragraph("Нет данных о доходах", bodyFont));
            log.debug("No incomes data available");
        }
    }

    private void addJobInformationSection(Document document, List<Pension> pensions, Head head,
                                          Industry industry, List<TurnoverRecord> turnoverRecords,
                                          Font bodyFont, Font headerFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding job information section");

        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Отрасль:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (industry != null && industry.getName() != null && !industry.getName().isEmpty()) {
            document.add(new Paragraph(industry.getName(), bodyFont));
            log.debug("Added industry information");
        } else {
            document.add(new Paragraph("Информация об отрасли отсутствует", bodyFont));
            log.debug("No industry information available");
        }

        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Пенсионные взносы:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (pensions != null && !pensions.isEmpty()) {
            addPensionsTable(document, pensions, bodyFont, subHeaderFont, tableFont);
            log.debug("Added pensions table");
        } else {
            document.add(new Paragraph("Нет данных о пенсионных взносах", bodyFont));
            log.debug("No pensions data available");
        }

        document.add(new Paragraph(" ", bodyFont));

        if (head != null && !head.isEmpty()) {
            addHeadInformationTable(document, head, bodyFont, subHeaderFont, tableFont);
            log.debug("Added head information table");
        } else {
            document.add(new Paragraph("Нет информации о руководящих позициях", bodyFont));
            log.debug("No head information available");
        }

        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Банковские счета:", headerFont));
        document.add(new Paragraph(" ", bodyFont));

        if (turnoverRecords != null && !turnoverRecords.isEmpty()) {
            addTurnoversTable(document, turnoverRecords, bodyFont, subHeaderFont, tableFont);
            log.debug("Added turnovers table");
        } else {
            document.add(new Paragraph("Нет информации о банковских счетах", bodyFont));
            log.debug("No turnover records available");
        }
    }

    private void addHeadInformationTable(Document document, Head head, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding head information table");

        if (head.getHead() != null && !head.getHead().isEmpty()) {
            document.add(new Paragraph("Руководящие должности:", subHeaderFont));

            PdfPTable supervisorTable = new PdfPTable(5);
            supervisorTable.setWidthPercentage(100);
            supervisorTable.setWidths(new float[]{2, 2, 2, 1.5f, 2.5f});
            supervisorTable.setSpacingBefore(5);
            supervisorTable.setSpacingAfter(10);

            addTableHeader(supervisorTable, "ИИН/БИН", subHeaderFont);
            addTableHeader(supervisorTable, "Тип позиции", subHeaderFont);
            addTableHeader(supervisorTable, "ИИН/БИН налогоплательщика", subHeaderFont);
            addTableHeader(supervisorTable, "Подставной владелец", subHeaderFont);
            addTableHeader(supervisorTable, "Наименование", subHeaderFont);

            for (SupervisorRecord supervisor : head.getHead()) {
                addTableCell(supervisorTable, supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "-", tableFont);
                addTableCell(supervisorTable, supervisor.getPositionType() != null ? supervisor.getPositionType() : "-", tableFont);
                addTableCell(supervisorTable, supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "-", tableFont);
                addTableCell(supervisorTable, supervisor.isNominal() ? "Да" : "Нет", tableFont);
                addTableCell(supervisorTable, supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "-", tableFont);
            }

            document.add(supervisorTable);
            log.debug("Added supervisor table");
        }

        document.add(new Paragraph("Финансовая информация:", subHeaderFont));
        document.add(new Paragraph("Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "-"), bodyFont));
        document.add(new Paragraph("Налоги: " + (head.getTax() != null ? head.getTax().toString() : "-"), bodyFont));
        log.debug("Added financial summary");

        if (head.getEsf() != null && !head.getEsf().isEmpty()) {
            document.add(new Paragraph("ESF информация:", subHeaderFont));

            PdfPTable esfTable = new PdfPTable(4);
            esfTable.setWidthPercentage(100);
            esfTable.setWidths(new float[]{2, 1.5f, 2, 1.5f});
            esfTable.setSpacingBefore(5);
            esfTable.setSpacingAfter(10);

            addTableHeader(esfTable, "ИИН/БИН", subHeaderFont);
            addTableHeader(esfTable, "Дата", subHeaderFont);
            addTableHeader(esfTable, "Активы", subHeaderFont);
            addTableHeader(esfTable, "Сумма", subHeaderFont);

            for (org.info.infobaza.model.info.active_income.EsfOverall esf : head.getEsf()) {
                addTableCell(esfTable, esf.getIin_bin() != null ? esf.getIin_bin() : "-", tableFont);
                addTableCell(esfTable, esf.getDate() != null ? esf.getDate().toString() : "-", tableFont);
                addTableCell(esfTable, esf.getAktivy() != null ? esf.getAktivy() : "-", tableFont);
                addTableCell(esfTable, esf.getSumm() != null ? esf.getSumm().toString() : "-", tableFont);
            }

            document.add(esfTable);
            log.debug("Added ESF table");
        }

        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            document.add(new Paragraph("Статусы:", subHeaderFont));
            document.add(new Paragraph(String.join(", ", head.getStatuses()), bodyFont));
            log.debug("Added statuses");
        }
    }

    private void addPensionsTable(Document document, List<Pension> pensions, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding pensions table");

        PdfPTable pensionsTable = new PdfPTable(7);
        pensionsTable.setWidthPercentage(100);
        pensionsTable.setWidths(new float[]{2, 2, 3, 2, 2, 3, 2});
        pensionsTable.setSpacingBefore(5);
        pensionsTable.setSpacingAfter(10);

        addTableHeader(pensionsTable, "Дата с", subHeaderFont);
        addTableHeader(pensionsTable, "Дата по", subHeaderFont);
        addTableHeader(pensionsTable, "Наименование", subHeaderFont);
        addTableHeader(pensionsTable, "P_RNN", subHeaderFont);
        addTableHeader(pensionsTable, "Максимальная з.п.", subHeaderFont);
        addTableHeader(pensionsTable, "Последняя з.п.", subHeaderFont);
        addTableHeader(pensionsTable, "Суммарно", subHeaderFont);

        for (Pension pension : pensions) {
            addTableCell(pensionsTable, pension.getDateFrom() != null ? pension.getDateFrom() : "-", tableFont);
            addTableCell(pensionsTable, pension.getDateTo() != null ? pension.getDateTo() : "-", tableFont);
            addTableCell(pensionsTable, pension.getName() != null ? pension.getName() : "-", tableFont);
            addTableCell(pensionsTable, pension.getP_RNN() != null ? pension.getP_RNN() : "-", tableFont);
            addTableCell(pensionsTable, pension.getMaxSalary() != null ? pension.getMaxSalary() : "-", tableFont);
            addTableCell(pensionsTable, pension.getLastSalary() != null ? pension.getLastSalary() : "-", tableFont);
            addTableCell(pensionsTable, pension.getSumm() != null ? pension.getSumm() : "-", tableFont);
        }

        document.add(pensionsTable);
    }

    private void addTurnoversTable(Document document, List<TurnoverRecord> turnoverRecords, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding turnovers table");

        PdfPTable turnoversTable = new PdfPTable(7);
        turnoversTable.setWidthPercentage(100);
        turnoversTable.setWidths(new float[]{2, 2, 3, 2, 2, 2, 2});
        turnoversTable.setSpacingBefore(5);
        turnoversTable.setSpacingAfter(10);

        addTableHeader(turnoversTable, "ИИН/БИН", subHeaderFont);
        addTableHeader(turnoversTable, "Название банка", subHeaderFont);
        addTableHeader(turnoversTable, "Счет", subHeaderFont);
        addTableHeader(turnoversTable, "Сумма", subHeaderFont);
        addTableHeader(turnoversTable, "Дата от", subHeaderFont);
        addTableHeader(turnoversTable, "Дата до", subHeaderFont);
        addTableHeader(turnoversTable, "Источник", subHeaderFont);

        for (TurnoverRecord turnoverRecord : turnoverRecords) {
            addTableCell(turnoversTable, turnoverRecord.getIinBin() != null ? turnoverRecord.getIinBin() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getBankName() != null ? turnoverRecord.getBankName() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getBankAccount() != null ? turnoverRecord.getBankAccount() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getSumm() != null ? turnoverRecord.getSumm() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getStartDate() != null ? turnoverRecord.getStartDate() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getEndDate() != null ? turnoverRecord.getEndDate() : "-", tableFont);
            addTableCell(turnoversTable, turnoverRecord.getSource() != null ? turnoverRecord.getSource() : "-", tableFont);
        }

        document.add(turnoversTable);
    }

    private void addRelationsTable(Document document, Map<String, List<RelationActive>> relationsMap, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding relations table");

        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            document.add(new Paragraph(category + ":", subHeaderFont));

            if (relations == null || relations.isEmpty()) {
                document.add(new Paragraph("  Нет связей в данной категории", bodyFont));
                document.add(new Paragraph(" ", bodyFont));
                continue;
            }

            PdfPTable relationsTable = new PdfPTable(7);
            relationsTable.setWidthPercentage(100);
            relationsTable.setWidths(new float[]{2.2f, 1.2f, 2.0f, 1.8f, 1.5f, 1.6f, 5.7f});
            relationsTable.setSpacingBefore(5);
            relationsTable.setSpacingAfter(10);

            addTableHeader(relationsTable, "ФИО", subHeaderFont);
            addTableHeader(relationsTable, "Связь", subHeaderFont);
            addTableHeader(relationsTable, "ИИН", subHeaderFont);
            addTableHeader(relationsTable, "Активы", subHeaderFont);
            addTableHeader(relationsTable, "Доходы", subHeaderFont);
            addTableHeader(relationsTable, "Сведения", subHeaderFont);
            addTableHeader(relationsTable, "Доп Инфо", subHeaderFont);

            for (RelationActive ra : relations) {
                addTableCell(relationsTable, ra.getFio() != null ? ra.getFio() : "-", tableFont);
                addTableCell(relationsTable, ra.getRelation() != null ? ra.getRelation() : "-", tableFont);
                addTableCell(relationsTable, ra.getIin() != null ? ra.getIin() : "-", tableFont);
                addTableCell(relationsTable, ra.getActives() != null ? ra.getActives() : "-", tableFont);
                addTableCell(relationsTable, ra.getIncomes() != null ? ra.getIncomes() : "-", tableFont);
                addTableCell(relationsTable, ra.getInfo() != null ? ra.getInfo() : "-", tableFont);

                String dopinfoStr = "-";

                if (ra.getDopinfo() != null && !ra.getDopinfo().isEmpty()) {
                    Map<String, String> dop = ra.getDopinfo();
                    Map<String, List<String>> splitted = new LinkedHashMap<>();
                    dop.forEach((k, v) -> {
                        if (!"vid_sviazi".equals(k)) {
                            splitted.put(k, v == null ? List.of() : Arrays.stream(v.split("\\|"))
                                    .map(String::trim)
                                    .filter(s -> !s.isBlank())
                                    .collect(Collectors.toList()));
                        }
                    });

                    int max = splitted.values().stream().mapToInt(List::size).max().orElse(0);
                    if (max > 0) {
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
                                    String formatted = String.format("%,d", Long.parseLong(sum))
                                            .replace(",", " ");
                                    sb.append(" • ").append(formatted).append(" ₸");
                                }
                            }

                            if (i < purposes.size() && !purposes.get(i).isBlank()) {
                                sb.append(" • ").append(purposes.get(i));
                            }

                            List<String> order = List.of(
                                    "AP code", "Registration date", "End registration date",
                                    "GRNZ", "Save begin date", "Save end date", "VIN code",
                                    "Summ", "For", "Number",
                                    "Tax for", "BVU", "Tax number", "UGD", "KNP", "KBK", "Purpose of tax",
                                    "For_dover", "Registration_date"
                            );

                            for (String key : order) {
                                if (splitted.containsKey(key) && i < splitted.get(key).size()) {
                                    String val = splitted.get(key).get(i);
                                    if (!val.isBlank()) {
                                        sb.append(" • ").append(RU.getOrDefault(key, key))
                                                .append(": ").append(val);
                                    }
                                }
                            }

                            lines.add(sb.toString());
                        }

                        dopinfoStr = String.join("\n", lines);
                    }
                }

                addTableCell(relationsTable, dopinfoStr, tableFont);
            }

            document.add(relationsTable);
            document.add(new Paragraph(" ", bodyFont));
        }
    }

    private void addActivesTable(Document document, List<RecordDt> recordsByOper, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding actives table");

        document.add(new Paragraph("Активы:", subHeaderFont));

        if (recordsByOper == null || recordsByOper.isEmpty()) {
            document.add(new Paragraph("  Нет записей для активов", bodyFont));
            document.add(new Paragraph(" ", bodyFont));
            log.debug("No actives records available");
            return;
        }

        boolean hasESFRecords = recordsByOper.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

        PdfPTable activesTable;
        if (hasESFRecords) {
            activesTable = new PdfPTable(9);
            activesTable.setWidthPercentage(100);
            activesTable.setWidths(new float[]{1.2f, 1.5f, 1.5f, 1.2f, 1.5f, 1.8f, 1.8f, 4f, 1.5f});
            activesTable.setSpacingBefore(5);
            activesTable.setSpacingAfter(10);

            addTableHeader(activesTable, "ИИН/БИН", subHeaderFont);
            addTableHeader(activesTable, "ИИН Покуп.", subHeaderFont);
            addTableHeader(activesTable, "ИИН Прод.", subHeaderFont);
            addTableHeader(activesTable, "Дата", subHeaderFont);
            addTableHeader(activesTable, "База данных", subHeaderFont);
            addTableHeader(activesTable, "Активы", subHeaderFont);
            addTableHeader(activesTable, "Операция", subHeaderFont);
            addTableHeader(activesTable, "Доп. инфо", subHeaderFont);
            addTableHeader(activesTable, "Сумма", subHeaderFont);

            for (RecordDt record : recordsByOper) {
                if (record instanceof ESFInformationRecordDt esfRecord) {
                    addTableCell(activesTable, esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getDate() != null ? esfRecord.getDate().toString() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getOper() != null ? esfRecord.getOper() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "-", tableFont);
                    addTableCell(activesTable, esfRecord.getSumm() != null ? esfRecord.getSumm() : "-", tableFont);
                } else if (record instanceof InformationRecordDt info) {
                    addTableCell(activesTable, info.getIin_bin() != null ? info.getIin_bin() : "-", tableFont);
                    addTableCell(activesTable, "-", tableFont);
                    addTableCell(activesTable, "-", tableFont);
                    addTableCell(activesTable, info.getDate() != null ? info.getDate().toString() : "-", tableFont);
                    addTableCell(activesTable, info.getDatabase() != null ? info.getDatabase() : "-", tableFont);
                    addTableCell(activesTable, info.getAktivy() != null ? info.getAktivy() : "-", tableFont);
                    addTableCell(activesTable, info.getOper() != null ? info.getOper() : "-", tableFont);
                    addTableCell(activesTable, info.getDopinfo() != null ? info.getDopinfo() : "-", tableFont);
                    addTableCell(activesTable, info.getSumm() != null ? info.getSumm() : "-", tableFont);
                } else if (record instanceof NaoConRecordDt info) {
                    addTableCell(activesTable, info.getIin_bin() != null ? info.getIin_bin() : "-", tableFont);
                    addTableCell(activesTable, "-", tableFont);
                    addTableCell(activesTable, "-", tableFont);
                    addTableCell(activesTable, info.getDate() != null ? info.getDate().toString() : "-", tableFont);
                    addTableCell(activesTable, info.getDatabase() != null ? info.getDatabase() : "-", tableFont);
                    addTableCell(activesTable, info.getAktivy() != null ? info.getAktivy() : "-", tableFont);
                    addTableCell(activesTable, info.getOper() != null ? info.getOper() : "-", tableFont);
                    addTableCell(activesTable, info.getDopinfo() != null ? info.getDopinfo() : "-", tableFont);
                    addTableCell(activesTable, info.getSumm() != null ? info.getSumm().toString() : "-", tableFont);
                }
            }
        } else {
            activesTable = new PdfPTable(6);
            activesTable.setWidthPercentage(100);
            activesTable.setWidths(new float[]{2, 1.5f, 2, 2, 2.5f, 1.5f});
            activesTable.setSpacingBefore(5);
            activesTable.setSpacingAfter(10);

            addTableHeader(activesTable, "ИИН/БИН", subHeaderFont);
            addTableHeader(activesTable, "Дата", subHeaderFont);
            addTableHeader(activesTable, "База данных", subHeaderFont);
            addTableHeader(activesTable, "Операция", subHeaderFont);
            addTableHeader(activesTable, "Доп. инфо", subHeaderFont);
            addTableHeader(activesTable, "Сумма", subHeaderFont);

            for (RecordDt record : recordsByOper) {
                addTableCell(activesTable, record.getIin_bin() != null ? record.getIin_bin() : "-", tableFont);
                addTableCell(activesTable, record.getDate() != null ? record.getDate().toString() : "-", tableFont);
                addTableCell(activesTable, record.getDatabase() != null ? record.getDatabase() : "-", tableFont);
                addTableCell(activesTable, record.getOper() != null ? record.getOper() : "-", tableFont);
                addTableCell(activesTable, record.getDopinfo() != null ? record.getDopinfo() : "-", tableFont);
                addTableCell(activesTable, record.getSumm() != null ? record.getSumm().toString() : "-", tableFont);
            }
        }

        document.add(activesTable);
        document.add(new Paragraph(" ", bodyFont));
        log.debug("Added actives table");
    }

    private void addIncomesTable(Document document, List<RecordDt> records, Font bodyFont, Font subHeaderFont, Font tableFont) throws DocumentException {
        log.debug("Adding incomes table");

        if (records == null || records.isEmpty()) {
            document.add(new Paragraph("  Нет записей о доходах", bodyFont));
            log.debug("No income records available");
            return;
        }

        PdfPTable incomesTable = new PdfPTable(6);
        incomesTable.setWidthPercentage(100);
        incomesTable.setWidths(new float[]{2, 1.5f, 2, 2, 2.5f, 1.5f});
        incomesTable.setSpacingBefore(5);
        incomesTable.setSpacingAfter(10);

        addTableHeader(incomesTable, "ИИН/БИН", subHeaderFont);
        addTableHeader(incomesTable, "Дата", subHeaderFont);
        addTableHeader(incomesTable, "База данных", subHeaderFont);
        addTableHeader(incomesTable, "Операция", subHeaderFont);
        addTableHeader(incomesTable, "Доп. инфо", subHeaderFont);
        addTableHeader(incomesTable, "Сумма", subHeaderFont);

        for (RecordDt record : records) {
            addTableCell(incomesTable, record.getIin_bin() != null ? record.getIin_bin() : "-", tableFont);
            addTableCell(incomesTable, record.getDate() != null ? record.getDate().toString() : "-", tableFont);
            addTableCell(incomesTable, record.getDatabase() != null ? record.getDatabase() : "-", tableFont);
            addTableCell(incomesTable, record.getOper() != null ? record.getOper() : "-", tableFont);
            addTableCell(incomesTable, record.getDopinfo() != null ? record.getDopinfo() : "-", tableFont);
            addTableCell(incomesTable, record.getSumm() != null ? record.getSumm().toString() : "-", tableFont);
        }

        document.add(incomesTable);
        log.debug("Added incomes table");
    }

    private void addTableHeader(PdfPTable table, String headerText, Font headerFont) {
        PdfPCell headerCell = new PdfPCell(new Phrase(headerText, headerFont));
        headerCell.setBackgroundColor(Color.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerCell.setPadding(6);
        table.addCell(headerCell);
        log.debug("Added table header: {}", headerText);
    }

    private void addTableCell(PdfPTable table, String text, Font tableFont) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", tableFont));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setNoWrap(false);
        cell.setLeading(14f, 0f);
        table.addCell(cell);
    }

    public BaseFont getBaseFont() throws IOException, DocumentException {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/kztimesnewroman.ttf");
            if (!fontResource.exists()) {
                log.error("Font file fonts/kztimesnewroman.ttf not found in resources");
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            }
            try (InputStream fontStream = fontResource.getInputStream()) {
                byte[] fontBytes = fontStream.readAllBytes();
                BaseFont baseFont = BaseFont.createFont("fonts/kztimesnewroman.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, fontBytes, null);
                log.debug("Loaded custom font: kztimesnewroman.ttf");
                return baseFont;
            }
        } catch (Exception e) {
            log.error("Error loading font, falling back to Helvetica: {}", e.getMessage());
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        }
    }

    private static class WatermarkPageEvent extends PdfPageEventHelper {
        private final Font watermarkFont;

        public WatermarkPageEvent(Font font) {
            this.watermarkFont = font;
            log.debug("Initialized WatermarkPageEvent");
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte canvas = writer.getDirectContentUnder();
                PdfGState gState = new PdfGState();
                gState.setFillOpacity(0.1f);
                canvas.setGState(gState);
                canvas.setColorFill(Color.GRAY);
                canvas.setFontAndSize(watermarkFont.getBaseFont(), 48);
                canvas.beginText();
                Rectangle page = document.getPageSize();
                float x = page.getWidth() / 2;
                float y = page.getHeight() / 2;
                canvas.showTextAligned(PdfContentByte.ALIGN_CENTER, "АФМ РК", x, y, 45);
                canvas.endText();
                log.debug("Added watermark to page");
            } catch (Exception e) {
                log.error("Error adding watermark: {}", e.getMessage());
            }
        }
    }
}