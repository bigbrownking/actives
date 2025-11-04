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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void exportToPdf(OutputStream outputStream, ExportRequest request) throws DocumentException {
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

        // Initialize fonts
        Font font;
        Font boldFont;
        try {
            BaseFont baseFont = getBaseFont();
            font = new Font(baseFont, 12);
            boldFont = new Font(baseFont, 12, Font.BOLD);
            log.debug("Fonts initialized successfully");
        } catch (Exception e) {
            log.error("Failed to load custom font, using default", e);
            font = new Font(Font.HELVETICA, 12);
            boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        }

        // Set watermark
        try {
            Font watermarkFont = new Font(getBaseFont(), 12);
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
                    document.add(new Paragraph("Данные для ИИН: " + iin, boldFont));
                    document.add(new Paragraph(" ", font));
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
                        document.add(new Paragraph("Error fetching relations for IIN: " + iin, font));
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
                        document.add(new Paragraph("Error fetching job information for IIN: " + iin, font));
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
                        document.add(new Paragraph("Error fetching active/income data for IIN: " + iin, font));
                        contentAdded = true;
                    } catch (Exception e) {
                        log.error("Error fetching active/income data for IIN {}: {}", iin, e.getMessage());
                        document.add(new Paragraph("Error fetching active/income data for IIN: " + iin, font));
                        contentAdded = true;
                    }

                    // Add sections
                    if (person != null) {
                        addPortraitSection(document, person, font);
                        contentAdded = true;
                        log.debug("Added portrait section for IIN: {}", iin);
                    }
                    addRelationsSection(document, personPrimaryRelations, personSecondaryRelations, font, boldFont);
                    addActivesAndIncomesSection(document, activeResponse, incomeResponse, font, boldFont);
                    addJobInformationSection(document, pensions, head, industry, turnoverRecords, font, boldFont);

                } catch (DocumentException e) {
                    log.error("DocumentException while processing IIN {}: {}", iin, e.getMessage());
                    document.add(new Paragraph("Error processing data for IIN: " + iin, font));
                    contentAdded = true;
                } catch (Exception e) {
                    log.error("Unexpected error processing IIN {}: {}", iin, e.getMessage());
                    document.add(new Paragraph("Unexpected error processing data for IIN: " + iin, font));
                    contentAdded = true;
                }
            }

            if (!contentAdded) {
                log.warn("No content added to PDF document for request: {}", request);
                document.add(new Paragraph("No data available for IIN: " + mainIin, font));
                log.info("Added fallback content for empty document");
            }

        } finally {
            document.close();
            log.debug("PDF document closed");
        }
        log.info("PDF export completed for IIN: {}", mainIin);
    }

    private void addPortraitSection(Document document, Person person, Font font) throws DocumentException {
        log.debug("Adding portrait section for person: {}", person != null ? person.getIin() : "null");
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3});

        addImageCell(table, person);
        addPersonInfoCell(table, person, font);

        document.add(table);
        document.add(new Paragraph(" ", font));
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

    private void addPersonInfoCell(PdfPTable table, Person person, Font font) {
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(PdfPCell.NO_BORDER);

        if (person != null) {
            textCell.addElement(new Paragraph(person.getFio() != null ? person.getFio() : "-", font));
            textCell.addElement(new Paragraph((person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен"), font));
            textCell.addElement(new Paragraph(person.getIin() != null ? person.getIin() : "-", font));
            textCell.addElement(new Paragraph((person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует"), font));
            textCell.addElement(new Paragraph(((person.getIsNominal() != null && person.getIsNominal()) ? "Номинал" : "Не номинал"), font));
            textCell.addElement(new Paragraph(person.getIsCryptoActive() ? "Есть переводы по криптовалюте" : "Нету переводов по криптовалюте"));
            log.debug("Added person info cell for IIN: {}", person.getIin());
        } else {
            textCell.addElement(new Paragraph("Person data not available", font));
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
                                     RelationActiveWithTypes secondaryRelations, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding relations section");

        document.add(new Paragraph("Первичные связи:", boldFont));
        document.add(new Paragraph(" ", font));

        if (primaryRelations != null && primaryRelations.getTypeToRelation() != null && !primaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, primaryRelations.getTypeToRelation(), font, boldFont);
            log.debug("Added primary relations table");
        } else {
            document.add(new Paragraph("Нет первичных связей", font));
            log.debug("No primary relations available");
        }

        document.add(new Paragraph(" ", font));

        document.add(new Paragraph("Вторичные связи:", boldFont));
        document.add(new Paragraph(" ", font));

        if (secondaryRelations != null && secondaryRelations.getTypeToRelation() != null && !secondaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, secondaryRelations.getTypeToRelation(), font, boldFont);
            log.debug("Added secondary relations table");
        } else {
            document.add(new Paragraph("Нет вторичных связей", font));
            log.debug("No secondary relations available");
        }

        document.add(new Paragraph(" ", font));
    }

    private void addActivesAndIncomesSection(Document document, ActiveWithRecords activeResponse,
                                             IncomeWithRecords incomeResponse, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding actives and incomes section");

        document.add(new Paragraph("Активы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()) {
            addActivesTable(document, activeResponse.getRecordsByOper(), font, boldFont);
            log.debug("Added actives table");
        } else {
            document.add(new Paragraph("Нет данных об активах", font));
            log.debug("No actives data available");
        }

        document.add(new Paragraph(" ", font));

        document.add(new Paragraph("Доходы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()) {
            addIncomesTable(document, incomeResponse.getRecordsByYear(), font, boldFont);
            log.debug("Added incomes table");
        } else {
            document.add(new Paragraph("Нет данных о доходах", font));
            log.debug("No incomes data available");
        }
    }

    private void addJobInformationSection(Document document, List<Pension> pensions, Head head,
                                          Industry industry, List<TurnoverRecord> turnoverRecords,
                                          Font font, Font boldFont) throws DocumentException {
        log.debug("Adding job information section");

        // Add Industry section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Отрасль:", boldFont));
        document.add(new Paragraph(" ", font));

        if (industry != null && industry.getName() != null && !industry.getName().isEmpty()) {
            document.add(new Paragraph(industry.getName(), font));
            log.debug("Added industry information");
        } else {
            document.add(new Paragraph("Информация об отрасли отсутствует", font));
            log.debug("No industry information available");
        }

        // Add Pensions section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Пенсионные взносы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (pensions != null && !pensions.isEmpty()) {
            addPensionsTable(document, pensions, font, boldFont);
            log.debug("Added pensions table");
        } else {
            document.add(new Paragraph("Нет данных о пенсионных взносах", font));
            log.debug("No pensions data available");
        }

        // Add Head section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Руководящие позиции:", boldFont));
        document.add(new Paragraph(" ", font));

        if (head != null && !head.isEmpty()) {
            addHeadInformationTable(document, head, font, boldFont);
            log.debug("Added head information table");
        } else {
            document.add(new Paragraph("Нет информации о руководящих позициях", font));
            log.debug("No head information available");
        }

        // Add Turnover section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Банковские счета:", boldFont));
        document.add(new Paragraph(" ", font));

        if (turnoverRecords != null && !turnoverRecords.isEmpty()) {
            addTurnoversTable(document, turnoverRecords, font, boldFont);
            log.debug("Added turnovers table");
        } else {
            document.add(new Paragraph("Нет информации о банковских счетах", font));
            log.debug("No turnover records available");
        }
    }

    private void addHeadInformationTable(Document document, Head head, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding head information table");

        // Add supervisor information
        if (head.getHead() != null && !head.getHead().isEmpty()) {
            document.add(new Paragraph("Руководящие должности:", boldFont));

            PdfPTable supervisorTable = new PdfPTable(5);
            supervisorTable.setWidthPercentage(100);
            supervisorTable.setWidths(new float[]{2, 2, 2, 1.5f, 2.5f});
            supervisorTable.setSpacingBefore(5);
            supervisorTable.setSpacingAfter(10);

            addTableHeader(supervisorTable, "ИИН/БИН", boldFont);
            addTableHeader(supervisorTable, "Тип позиции", boldFont);
            addTableHeader(supervisorTable, "ИИН/БИН налогоплательщика", boldFont);
            addTableHeader(supervisorTable, "Тип", boldFont);
            addTableHeader(supervisorTable, "Наименование", boldFont);

            for (SupervisorRecord supervisor : head.getHead()) {
                addTableCell(supervisorTable, supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "-", font);
                addTableCell(supervisorTable, supervisor.getPositionType() != null ? supervisor.getPositionType() : "-", font);
                addTableCell(supervisorTable, supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "-", font);
            //    addTableCell(supervisorTable, supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "-", font);
                addTableCell(supervisorTable, supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "-", font);
            }

            document.add(supervisorTable);
            log.debug("Added supervisor table");
        }

        // Add financial summary
        document.add(new Paragraph("Финансовая информация:", boldFont));
        document.add(new Paragraph("Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "-"), font));
        document.add(new Paragraph("Налоги: " + (head.getTax() != null ? head.getTax().toString() : "-"), font));
        log.debug("Added financial summary");

        // Add ESF information
        if (head.getEsf() != null && !head.getEsf().isEmpty()) {
            document.add(new Paragraph("ESF информация:", boldFont));

            PdfPTable esfTable = new PdfPTable(4);
            esfTable.setWidthPercentage(100);
            esfTable.setWidths(new float[]{2, 1.5f, 2, 1.5f});
            esfTable.setSpacingBefore(5);
            esfTable.setSpacingAfter(10);

            addTableHeader(esfTable, "ИИН/БИН", boldFont);
            addTableHeader(esfTable, "Дата", boldFont);
            addTableHeader(esfTable, "Активы", boldFont);
            addTableHeader(esfTable, "Сумма", boldFont);

            for (org.info.infobaza.model.info.active_income.EsfOverall esf : head.getEsf()) {
                addTableCell(esfTable, esf.getIin_bin() != null ? esf.getIin_bin() : "-", font);
                addTableCell(esfTable, esf.getDate() != null ? esf.getDate().toString() : "-", font);
                addTableCell(esfTable, esf.getAktivy() != null ? esf.getAktivy() : "-", font);
                addTableCell(esfTable, esf.getSumm() != null ? esf.getSumm().toString() : "-", font);
            }

            document.add(esfTable);
            log.debug("Added ESF table");
        }

        // Add statuses
        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            document.add(new Paragraph("Статусы:", boldFont));
            document.add(new Paragraph(String.join(", ", head.getStatuses()), font));
            log.debug("Added statuses");
        }
    }

    private void addPensionsTable(Document document, List<Pension> pensions, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding pensions table");

        PdfPTable pensionsTable = new PdfPTable(7);
        pensionsTable.setWidthPercentage(100);
        pensionsTable.setWidths(new float[]{2, 2, 3, 2, 2, 3, 2});
        pensionsTable.setSpacingBefore(5);
        pensionsTable.setSpacingAfter(10);

        addTableHeader(pensionsTable, "Дата с", boldFont);
        addTableHeader(pensionsTable, "Дата по", boldFont);
        addTableHeader(pensionsTable, "Наименование", boldFont);
        addTableHeader(pensionsTable, "P_RNN", boldFont);
        addTableHeader(pensionsTable, "Максимальная з.п.", boldFont);
        addTableHeader(pensionsTable, "Последняя з.п.", boldFont);
        addTableHeader(pensionsTable, "Суммарно", boldFont);

        for (Pension pension : pensions) {
            addTableCell(pensionsTable, pension.getDateFrom() != null ? pension.getDateFrom() : "-", font);
            addTableCell(pensionsTable, pension.getDateTo() != null ? pension.getDateTo() : "-", font);
            addTableCell(pensionsTable, pension.getName() != null ? pension.getName() : "-", font);
            addTableCell(pensionsTable, pension.getP_RNN() != null ? pension.getP_RNN() : "-", font);
            addTableCell(pensionsTable, pension.getMaxSalary() != null ? pension.getMaxSalary() : "-", font);
            addTableCell(pensionsTable, pension.getLastSalary() != null ? pension.getLastSalary() : "-", font);
            addTableCell(pensionsTable, pension.getSumm() != null ? pension.getSumm() : "-", font);
        }

        document.add(pensionsTable);
    }
    private void addTurnoversTable(Document document, List<TurnoverRecord> turnoverRecords, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding turnovers table");

        PdfPTable turnoversTable = new PdfPTable(7);
        turnoversTable.setWidthPercentage(100);
        turnoversTable.setWidths(new float[]{2, 2, 3, 2, 2, 2, 2});
        turnoversTable.setSpacingBefore(5);
        turnoversTable.setSpacingAfter(10);

        addTableHeader(turnoversTable, "ИИН/БИН", boldFont);
        addTableHeader(turnoversTable, "Название банка", boldFont);
        addTableHeader(turnoversTable, "Счет", boldFont);
        addTableHeader(turnoversTable, "Сумма", boldFont);
        addTableHeader(turnoversTable, "Дата от", boldFont);
        addTableHeader(turnoversTable, "Дата до", boldFont);
        addTableHeader(turnoversTable, "Источник", boldFont);

        for (TurnoverRecord turnoverRecord : turnoverRecords) {
            addTableCell(turnoversTable, turnoverRecord.getIinBin() != null ? turnoverRecord.getIinBin() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getBankName() != null ? turnoverRecord.getBankName() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getBankAccount() != null ? turnoverRecord.getBankAccount() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getSumm() != null ? turnoverRecord.getSumm() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getStartDate() != null ? turnoverRecord.getStartDate() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getEndDate() != null ? turnoverRecord.getEndDate() : "-", font);
            addTableCell(turnoversTable, turnoverRecord.getSource() != null ? turnoverRecord.getSource() : "-", font);
        }

        document.add(turnoversTable);
    }

    private void addRelationsTable(Document document, Map<String, List<RelationActive>> relationsMap, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding relations table");

        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            document.add(new Paragraph(category + ":", boldFont));

            if (relations == null || relations.isEmpty()) {
                document.add(new Paragraph("  Нет связей в данной категории", font));
                document.add(new Paragraph(" ", font));
                log.debug("No relations in category: {}", category);
                continue;
            }

            PdfPTable relationsTable = new PdfPTable(7);
            relationsTable.setWidthPercentage(100);
            relationsTable.setWidths(new float[]{2.6f, 1.2f, 2.2f, 2, 1.5f, 1.8f, 2});
            relationsTable.setSpacingBefore(5);
            relationsTable.setSpacingAfter(10);

            addTableHeader(relationsTable, "ФИО", boldFont);
            addTableHeader(relationsTable, "Связь", boldFont);
            addTableHeader(relationsTable, "ИИН", boldFont);
            addTableHeader(relationsTable, "Активы", boldFont);
            addTableHeader(relationsTable, "Доходы", boldFont);
            addTableHeader(relationsTable, "Номинал", boldFont);
            addTableHeader(relationsTable, "Доп Инфо", boldFont);

            for (RelationActive ra : relations) {
                log.debug("Processing RelationActive for IIN: {}, dopinfo: {}", ra.getIin(), ra.getDopinfo());
                addTableCell(relationsTable, ra.getFio() != null ? ra.getFio() : "-", font);
                addTableCell(relationsTable, ra.getRelation() != null ? ra.getRelation() : "-", font);
                addTableCell(relationsTable, ra.getIin() != null ? ra.getIin() : "-", font);
                addTableCell(relationsTable, ra.getActives() != null ? ra.getActives() : "-", font);
                addTableCell(relationsTable, ra.getIncomes() != null ? ra.getIncomes() : "-", font);
                addTableCell(relationsTable, ra.isNominal() ? "Да" : "Нет", font);

                String dopinfoStr = "-";
                if (ra.getDopinfo() != null && !ra.getDopinfo().isEmpty()) {
                    dopinfoStr = ra.getDopinfo().entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("; "));
                }
                addTableCell(relationsTable, dopinfoStr, font);
            }

            document.add(relationsTable);
            log.debug("Added relations table for category: {}", category);
        }
    }

    private void addActivesTable(Document document, List<RecordDt> recordsByOper, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding actives table");

        document.add(new Paragraph("Активы:", boldFont));

        if (recordsByOper == null || recordsByOper.isEmpty()) {
            document.add(new Paragraph("  Нет записей для активов", font));
            document.add(new Paragraph(" ", font));
            log.debug("No actives records available");
            return;
        }

        boolean hasESFRecords = recordsByOper.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

        PdfPTable activesTable;
        if (hasESFRecords) {
            activesTable = new PdfPTable(9);
            activesTable.setWidthPercentage(100);
            activesTable.setWidths(new float[]{2.2f, 1.5f, 1.5f, 1.2f, 1.5f, 1.8f, 1.8f, 2f, 1.5f});
            activesTable.setSpacingBefore(5);
            activesTable.setSpacingAfter(10);

            addTableHeader(activesTable, "ИИН/БИН", boldFont);
            addTableHeader(activesTable, "ИИН Покуп.", boldFont);
            addTableHeader(activesTable, "ИИН Прод.", boldFont);
            addTableHeader(activesTable, "Дата", boldFont);
            addTableHeader(activesTable, "База данных", boldFont);
            addTableHeader(activesTable, "Активы", boldFont);
            addTableHeader(activesTable, "Операция", boldFont);
            addTableHeader(activesTable, "Доп. инфо", boldFont);
            addTableHeader(activesTable, "Сумма", boldFont);

            for (RecordDt record : recordsByOper) {
                if (record instanceof ESFInformationRecordDt esfRecord) {
                    addTableCell(activesTable, esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "-", font);
                    addTableCell(activesTable, esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "-", font);
                    addTableCell(activesTable, esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "-", font);
                    addTableCell(activesTable, esfRecord.getDate() != null ? esfRecord.getDate().toString() : "-", font);
                    addTableCell(activesTable, esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "-", font);
                    addTableCell(activesTable, esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "-", font);
                    addTableCell(activesTable, esfRecord.getOper() != null ? esfRecord.getOper() : "-", font);
                    addTableCell(activesTable, esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "-", font);
                    addTableCell(activesTable, esfRecord.getSumm() != null ? esfRecord.getSumm() : "-", font);
                } else if (record instanceof InformationRecordDt info) {
                    addTableCell(activesTable, info.getIin_bin() != null ? info.getIin_bin() : "-", font);
                    addTableCell(activesTable, "-", font); // ИИН Покуп.
                    addTableCell(activesTable, "-", font); // ИИН Прод.
                    addTableCell(activesTable, info.getDate() != null ? info.getDate().toString() : "-", font);
                    addTableCell(activesTable, info.getDatabase() != null ? info.getDatabase() : "-", font);
                    addTableCell(activesTable, info.getAktivy() != null ? info.getAktivy() : "-", font);
                    addTableCell(activesTable, info.getOper() != null ? info.getOper() : "-", font);
                    addTableCell(activesTable, info.getDopinfo() != null ? info.getDopinfo() : "-", font);
                    addTableCell(activesTable, info.getSumm() != null ? info.getSumm() : "-", font);
                } else if (record instanceof NaoConRecordDt info) {
                    addTableCell(activesTable, info.getIin_bin() != null ? info.getIin_bin() : "-", font);
                    addTableCell(activesTable, "-", font); // ИИН Покуп.
                    addTableCell(activesTable, "-", font); // ИИН Прод.
                    addTableCell(activesTable, info.getDate() != null ? info.getDate().toString() : "-", font);
                    addTableCell(activesTable, info.getDatabase() != null ? info.getDatabase() : "-", font);
                    addTableCell(activesTable, info.getAktivy() != null ? info.getAktivy() : "-", font);
                    addTableCell(activesTable, info.getOper() != null ? info.getOper() : "-", font);
                    addTableCell(activesTable, info.getDopinfo() != null ? info.getDopinfo() : "-", font);
                    addTableCell(activesTable, info.getSumm() != null ? info.getSumm().toString() : "-", font);
                }
            }
        } else {
            activesTable = new PdfPTable(6);
            activesTable.setWidthPercentage(100);
            activesTable.setWidths(new float[]{2, 1.5f, 2, 2, 2.5f, 1.5f});
            activesTable.setSpacingBefore(5);
            activesTable.setSpacingAfter(10);

            addTableHeader(activesTable, "ИИН/БИН", boldFont);
            addTableHeader(activesTable, "Дата", boldFont);
            addTableHeader(activesTable, "База данных", boldFont);
            addTableHeader(activesTable, "Операция", boldFont);
            addTableHeader(activesTable, "Доп. инфо", boldFont);
            addTableHeader(activesTable, "Сумма", boldFont);

            for (RecordDt record : recordsByOper) {
                addTableCell(activesTable, record.getIin_bin() != null ? record.getIin_bin() : "-", font);
                addTableCell(activesTable, record.getDate() != null ? record.getDate().toString() : "-", font);
                addTableCell(activesTable, record.getDatabase() != null ? record.getDatabase() : "-", font);
                addTableCell(activesTable, record.getOper() != null ? record.getOper() : "-", font);
                addTableCell(activesTable, record.getDopinfo() != null ? record.getDopinfo() : "-", font);
                addTableCell(activesTable, record.getSumm() != null ? record.getSumm().toString() : "-", font);
            }
        }

        document.add(activesTable);
        document.add(new Paragraph(" ", font));
        log.debug("Added actives table");
    }

    private void addIncomesTable(Document document, List<RecordDt> records, Font font, Font boldFont) throws DocumentException {
        log.debug("Adding incomes table");

        if (records == null || records.isEmpty()) {
            document.add(new Paragraph("  Нет записей о доходах", font));
            log.debug("No income records available");
            return;
        }

        PdfPTable incomesTable = new PdfPTable(6);
        incomesTable.setWidthPercentage(100);
        incomesTable.setWidths(new float[]{2, 1.5f, 2, 2, 2.5f, 1.5f});
        incomesTable.setSpacingBefore(5);
        incomesTable.setSpacingAfter(10);

        addTableHeader(incomesTable, "ИИН/БИН", boldFont);
        addTableHeader(incomesTable, "Дата", boldFont);
        addTableHeader(incomesTable, "База данных", boldFont);
        addTableHeader(incomesTable, "Операция", boldFont);
        addTableHeader(incomesTable, "Доп. инфо", boldFont);
        addTableHeader(incomesTable, "Сумма", boldFont);

        for (RecordDt record : records) {
            addTableCell(incomesTable, record.getIin_bin() != null ? record.getIin_bin() : "-", font);
            addTableCell(incomesTable, record.getDate() != null ? record.getDate().toString() : "-", font);
            addTableCell(incomesTable, record.getDatabase() != null ? record.getDatabase() : "-", font);
            addTableCell(incomesTable, record.getOper() != null ? record.getOper() : "-", font);
            addTableCell(incomesTable, record.getDopinfo() != null ? record.getDopinfo() : "-", font);
            addTableCell(incomesTable, record.getSumm() != null ? record.getSumm().toString() : "-", font);
        }

        document.add(incomesTable);
        log.debug("Added incomes table");
    }

    private void addTableHeader(PdfPTable table, String headerText, Font boldFont) {
        PdfPCell headerCell = new PdfPCell(new Phrase(headerText, boldFont));
        headerCell.setBackgroundColor(Color.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerCell.setPadding(8);
        table.addCell(headerCell);
        log.debug("Added table header: {}", headerText);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
        log.debug("Added table cell: {}", text);
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

                // Get page dimensions
                Rectangle pageSize = document.getPageSize();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                // Set transparency
                PdfGState gState = new PdfGState();
                gState.setFillOpacity(0.1f); // 10% opacity
                canvas.setGState(gState);

                // Set font and color
                canvas.setFontAndSize(watermarkFont.getBaseFont(), 48);
                canvas.setColorFill(Color.GRAY);

                // Define the diagonal from bottom-left (margin) to top-right (margin)
                float margin = 50f; // Margin from edges
                float endX = pageWidth - margin;
                float endY = pageHeight - margin;

                // Number of watermark instances
                int numWatermarks = 5; // Adjust as needed
                float deltaX = (endX - margin) / (numWatermarks - 1);
                float deltaY = (endY - margin) / (numWatermarks - 1);

                // Draw watermarks along the diagonal
                canvas.beginText();
                for (int i = 0; i < numWatermarks; i++) {
                    float x = margin + i * deltaX;
                    float y = margin + i * deltaY;
                    canvas.showTextAligned(PdfContentByte.ALIGN_CENTER, "АФМ РК", x, y, 45);
                }
                canvas.endText();
                log.debug("Added watermark to page");
            } catch (Exception e) {
                log.error("Error adding watermark: {}", e.getMessage());
            }
        }
    }
}