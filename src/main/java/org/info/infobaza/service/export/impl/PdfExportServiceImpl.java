package org.info.infobaza.service.export.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.service.Analyzer;
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
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    @Override
    public void exportToPdf(OutputStream outputStream, ExportRequest request) throws IOException, NotFoundException, DocumentException {
        log.info("REQUEST IS : {}", request);
        String dateFrom = request.getDateFrom().toString();
        String dateTo = request.getDateTo().toString();
        String iin = request.getIin();
        List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
        List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();


        // Fetch Person data
        Person person = portretService.getPerson(iin);

        // Fetch Relations data
        RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                iin,
                dateFrom, dateTo);

        RelationActiveWithTypes secondaryRelations = relationService.getSecondaryRelationsOfPerson(
                iin,
                dateFrom, dateTo);

        // Fetch job information
        List<Pension> pensions = enpfService.getPension(
                iin,
                dateFrom, dateTo
        );

        Head head = headService.constructHead(
                iin,
                dateFrom, dateTo
        );

        Industry industry = industrialService.getIndustry(
                iin
        );

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

        // Create PDF document
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        try {
            Font watermarkFont = new Font(getBaseFont(), 12);
            writer.setPageEvent(new WatermarkPageEvent(watermarkFont));
        } catch (Exception e) {
            log.warn("Could not set up watermark: " + e.getMessage());
        }

        document.open();

        try {
            Font font = new Font(getBaseFont(), 12);
            Font boldFont = new Font(getBaseFont(), 12, Font.BOLD);

            addPortraitSection(document, person, font);
            addRelationsSection(document, primaryRelations, secondaryRelations, font, boldFont);
            addActivesAndIncomesSection(document, activeResponse, incomeResponse, font, boldFont);
            addJobInformationSection(document, pensions, head, industry, font, boldFont);

        } finally {
            document.close();
        }
    }

    private void addPortraitSection(Document document, Person person, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addImageCell(table, person);
        addPersonInfoCell(table, person, font);

        document.add(table);
        document.add(new Paragraph(" ", font));
    }

    private void addImageCell(PdfPTable table, Person person) {
        if (person.getImage() != null && !person.getImage().isEmpty()) {
            try {
                String imageData = person.getImage().replaceFirst("^data:image/[^;]+;base64,?", "");
                byte[] imageBytes = Base64.getDecoder().decode(imageData);
                Image image = Image.getInstance(imageBytes);
                image.scaleToFit(100, 100);

                PdfPCell imageCell = new PdfPCell(image);
                imageCell.setBorder(PdfPCell.NO_BORDER);
                imageCell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
                table.addCell(imageCell);
            } catch (Exception e) {
                log.error("Failed to add image to PDF: " + e.getMessage());
                addEmptyCell(table);
            }
        } else {
            addEmptyCell(table);
        }
    }

    private void addPersonInfoCell(PdfPTable table, Person person, Font font) {
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(PdfPCell.NO_BORDER);

        textCell.addElement(new Paragraph(person.getFio() != null ? person.getFio() : "N/A", font));
        textCell.addElement(new Paragraph((person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен"), font));
        textCell.addElement(new Paragraph(person.getIin() != null ? person.getIin() : "N/A", font));
        textCell.addElement(new Paragraph((person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует"), font));
        textCell.addElement(new Paragraph(((person.getIsNominal() != null && person.getIsNominal()) ? "Номинал" : "Не номинал"), font));

        table.addCell(textCell);
    }

    private void addEmptyCell(PdfPTable table) {
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(emptyCell);
    }

    private void addRelationsSection(Document document, RelationActiveWithTypes primaryRelations,
                                     RelationActiveWithTypes secondaryRelations, Font font, Font boldFont) throws DocumentException {

        document.add(new Paragraph("Первичные связи:", boldFont));
        document.add(new Paragraph(" ", font));

        if (primaryRelations != null && primaryRelations.getTypeToRelation() != null && !primaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, primaryRelations.getTypeToRelation(), font, boldFont);
        } else {
            document.add(new Paragraph("Нет первичных связей", font));
        }

        document.add(new Paragraph(" ", font));

        document.add(new Paragraph("Вторичные связи:", boldFont));
        document.add(new Paragraph(" ", font));

        if (secondaryRelations != null && secondaryRelations.getTypeToRelation() != null && !secondaryRelations.getTypeToRelation().isEmpty()) {
            addRelationsTable(document, secondaryRelations.getTypeToRelation(), font, boldFont);
        } else {
            document.add(new Paragraph("Нет вторичных связей", font));
        }

        document.add(new Paragraph(" ", font));
    }

    private void addActivesAndIncomesSection(Document document, ActiveWithRecords activeResponse,
                                             IncomeWithRecords incomeResponse, Font font, Font boldFont) throws DocumentException {

        document.add(new Paragraph("Активы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()) {
            addActivesTable(document, activeResponse.getRecordsByOper(), font, boldFont);
        } else {
            document.add(new Paragraph("Нет данных об активах", font));
        }

        document.add(new Paragraph(" ", font));

        document.add(new Paragraph("Доходы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()) {
            addIncomesTable(document, incomeResponse.getRecordsByYear(), font, boldFont);
        } else {
            document.add(new Paragraph("Нет данных о доходах", font));
        }
    }

    private void addJobInformationSection(Document document, List<Pension> pensions, Head head,
                                          Industry industry, Font font, Font boldFont) throws DocumentException {
        // Add Industry section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Отрасль:", boldFont));
        document.add(new Paragraph(" ", font));

        if (industry != null && industry.getName() != null && !industry.getName().isEmpty()) {
            document.add(new Paragraph(industry.getName(), font));
        } else {
            document.add(new Paragraph("Информация об отрасли отсутствует", font));
        }

        // Add Head section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Руководящие позиции:", boldFont));
        document.add(new Paragraph(" ", font));

        if (!head.isEmpty()) {
            document.add(new Paragraph(" ", font));
            addHeadInformationTable(document, head, font, boldFont);
        } else {
            document.add(new Paragraph("Нет информации о руководящих позициях", font));
        }

        // Add Pensions section
        document.add(new Paragraph(" ", font));
        document.add(new Paragraph("Пенсионные взносы:", boldFont));
        document.add(new Paragraph(" ", font));

        if (pensions != null && !pensions.isEmpty()) {
            addPensionsTable(document, pensions, font, boldFont);
        } else {
            document.add(new Paragraph("Нет данных о пенсионных взносах", font));
        }
    }

    private void addHeadInformationTable(Document document, Head head, Font font, Font boldFont) throws DocumentException {

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
                addTableCell(supervisorTable, supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "N/A", font);
                addTableCell(supervisorTable, supervisor.getPositionType() != null ? supervisor.getPositionType() : "N/A", font);
                addTableCell(supervisorTable, supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "N/A", font);
                addTableCell(supervisorTable, supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "N/A", font);
                addTableCell(supervisorTable, supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "N/A", font);
            }

            document.add(supervisorTable);
        }

        // Add company information
        if (head.getOked() != null && !head.getOked().isEmpty()) {
            document.add(new Paragraph("Компании:", boldFont));

            PdfPTable companyTable = new PdfPTable(5);
            companyTable.setWidthPercentage(100);
            companyTable.setWidths(new float[]{3, 3, 2, 1.5f, 2f});
            companyTable.setSpacingBefore(5);
            companyTable.setSpacingAfter(10);

            addTableHeader(companyTable, "Русское название", boldFont);
            addTableHeader(companyTable, "Оригинальное название", boldFont);
            addTableHeader(companyTable, "БИН", boldFont);
            addTableHeader(companyTable, "Дата регистрации", boldFont);
            addTableHeader(companyTable, "Телефон", boldFont);

            for (CompanyRecord company : head.getOked()) {
                addTableCell(companyTable, company.getRusName() != null ? company.getRusName() : "N/A", font);
                addTableCell(companyTable, company.getOrigName() != null ? company.getOrigName() : "N/A", font);
                addTableCell(companyTable, company.getBin() != null ? company.getBin() : "N/A", font);
                addTableCell(companyTable, company.getDateReg() != null ? company.getDateReg().toString() : "N/A", font);
                addTableCell(companyTable, company.getTelephone() != null ? company.getTelephone() : "N/A", font);
            }

            document.add(companyTable);
        }

        // Add financial summary
        document.add(new Paragraph("Финансовая информация:", boldFont));
        document.add(new Paragraph("Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "N/A"), font));
        document.add(new Paragraph("Налоги: " + (head.getTax() != null ? head.getTax().toString() : "N/A"), font));

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
                addTableCell(esfTable, esf.getIin_bin() != null ? esf.getIin_bin() : "N/A", font);
                addTableCell(esfTable, esf.getDate() != null ? esf.getDate().toString() : "N/A", font);
                addTableCell(esfTable, esf.getAktivy() != null ? esf.getAktivy() : "N/A", font);
                addTableCell(esfTable, esf.getSumm() != null ? esf.getSumm().toString() : "N/A", font);
            }

            document.add(esfTable);
        }

        // Add statuses
        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            document.add(new Paragraph("Статусы:", boldFont));
            document.add(new Paragraph(String.join(", ", head.getStatuses()), font));
        }
    }

    private void addPensionsTable(Document document, List<Pension> pensions, Font font, Font boldFont) throws DocumentException {
        PdfPTable pensionsTable = new PdfPTable(4);
        pensionsTable.setWidthPercentage(100);
        pensionsTable.setWidths(new float[]{2, 2, 3, 2});
        pensionsTable.setSpacingBefore(5);
        pensionsTable.setSpacingAfter(10);

        addTableHeader(pensionsTable, "Дата с", boldFont);
        addTableHeader(pensionsTable, "Дата по", boldFont);
        addTableHeader(pensionsTable, "Наименование", boldFont);
        addTableHeader(pensionsTable, "P_RNN", boldFont);

        for (Pension pension : pensions) {
            addTableCell(pensionsTable, pension.getDateFrom() != null ? pension.getDateFrom() : "N/A", font);
            addTableCell(pensionsTable, pension.getDateTo() != null ? pension.getDateTo() : "N/A", font);
            addTableCell(pensionsTable, pension.getName() != null ? pension.getName() : "N/A", font);
            addTableCell(pensionsTable, pension.getP_RNN() != null ? pension.getP_RNN() : "N/A", font);
        }

        document.add(pensionsTable);
    }

    private void addRelationsTable(Document document, Map<String, List<RelationActive>> relationsMap, Font font, Font boldFont) throws DocumentException {
        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            document.add(new Paragraph(category + ":", boldFont));

            if (relations == null || relations.isEmpty()) {
                document.add(new Paragraph("  Нет связей в данной категории", font));
                document.add(new Paragraph(" ", font));
                continue;
            }

            PdfPTable relationsTable = new PdfPTable(6);
            relationsTable.setWidthPercentage(100);
            relationsTable.setWidths(new float[]{2.5f, 1.5f, 1.5f, 2, 1.2f, 1.2f});
            relationsTable.setSpacingBefore(5);
            relationsTable.setSpacingAfter(10);

            addTableHeader(relationsTable, "ФИО", boldFont);
            addTableHeader(relationsTable, "Связь", boldFont);
            addTableHeader(relationsTable, "ИИН", boldFont);
            addTableHeader(relationsTable, "Активы", boldFont);
            addTableHeader(relationsTable, "Доходы", boldFont);
            addTableHeader(relationsTable, "Номинал", boldFont);

            for (RelationActive ra : relations) {
                addTableCell(relationsTable, ra.getFio() != null ? ra.getFio() : "N/A", font);
                addTableCell(relationsTable, ra.getRelation() != null ? ra.getRelation() : "N/A", font);
                addTableCell(relationsTable, ra.getIin() != null ? ra.getIin() : "N/A", font);
                addTableCell(relationsTable, ra.getActives() != null ? ra.getActives() : "N/A", font);
                addTableCell(relationsTable, ra.getIncomes() != null ? ra.getIncomes() : "N/A", font);
                addTableCell(relationsTable, ra.isNominal() ? "Да": "Нет", font);
            }

            document.add(relationsTable);
        }
    }

    private void addActivesTable(Document document, Map<String, List<RecordDt>> recordsByOper, Font font, Font boldFont) throws DocumentException {
        for (Map.Entry<String, List<RecordDt>> entry : recordsByOper.entrySet()) {
            String operation = entry.getKey();
            List<RecordDt> records = entry.getValue();

            document.add(new Paragraph(operation + ":", boldFont));

            if (records == null || records.isEmpty()) {
                document.add(new Paragraph("  Нет записей для данной операции", font));
                document.add(new Paragraph(" ", font));
                continue;
            }

            boolean hasESFRecords = records.stream().anyMatch(record -> record instanceof org.info.infobaza.model.info.active_income.ESFInformationRecordDt);

            PdfPTable activesTable;
            if (hasESFRecords) {
                activesTable = new PdfPTable(9);
                activesTable.setWidthPercentage(100);
                activesTable.setWidths(new float[]{1.5f, 1.5f, 1.5f, 1.2f, 1.5f, 1.5f, 1.5f, 2f, 1.2f});
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

                for (RecordDt record : records) {
                    if (record instanceof org.info.infobaza.model.info.active_income.ESFInformationRecordDt) {
                        org.info.infobaza.model.info.active_income.ESFInformationRecordDt esfRecord =
                                (org.info.infobaza.model.info.active_income.ESFInformationRecordDt) record;

                        addTableCell(activesTable, esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getDate() != null ? esfRecord.getDate().toString() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getOper() != null ? esfRecord.getOper() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "N/A", font);
                        addTableCell(activesTable, esfRecord.getSumm() != null ? esfRecord.getSumm() : "N/A", font);
                    } else {
                        addTableCell(activesTable, record.getIin_bin() != null ? record.getIin_bin() : "N/A", font);
                        addTableCell(activesTable, "N/A", font); // ИИН Покуп.
                        addTableCell(activesTable, "N/A", font); // ИИН Прод.
                        addTableCell(activesTable, record.getDate() != null ? record.getDate().toString() : "N/A", font);
                        addTableCell(activesTable, record.getDatabase() != null ? record.getDatabase() : "N/A", font);
                        addTableCell(activesTable, "N/A", font); // Активы (not available in InformationRecordDt)
                        addTableCell(activesTable, record.getOper() != null ? record.getOper() : "N/A", font);
                        addTableCell(activesTable, record.getDopinfo() != null ? record.getDopinfo() : "N/A", font);
                        addTableCell(activesTable, record.getSumm() != null ? record.getSumm() : "N/A", font);
                    }
                }

            } else {
                activesTable = new PdfPTable(6);
                activesTable.setWidthPercentage(100);
                activesTable.setWidths(new float[]{2, 1.5f, 2, 2, 2.5f, 1.5f});
                activesTable.setSpacingBefore(5);
                activesTable.setSpacingAfter(10);

                // Add regular table headers
                addTableHeader(activesTable, "ИИН/БИН", boldFont);
                addTableHeader(activesTable, "Дата", boldFont);
                addTableHeader(activesTable, "База данных", boldFont);
                addTableHeader(activesTable, "Операция", boldFont);
                addTableHeader(activesTable, "Доп. инфо", boldFont);
                addTableHeader(activesTable, "Сумма", boldFont);

                // Add regular records data
                for (RecordDt record : records) {
                    addTableCell(activesTable, record.getIin_bin() != null ? record.getIin_bin() : "N/A", font);
                    addTableCell(activesTable, record.getDate() != null ? record.getDate().toString() : "N/A", font);
                    addTableCell(activesTable, record.getDatabase() != null ? record.getDatabase() : "N/A", font);
                    addTableCell(activesTable, record.getOper() != null ? record.getOper() : "N/A", font);
                    addTableCell(activesTable, record.getDopinfo() != null ? record.getDopinfo() : "N/A", font);
                    addTableCell(activesTable, record.getSumm() != null ? record.getSumm() : "N/A", font);
                }

            }
            document.add(activesTable);
        }
    }

    private void addIncomesTable(Document document, List<RecordDt> records, Font font, Font boldFont) throws DocumentException {
        if (records == null || records.isEmpty()) {
            document.add(new Paragraph("  Нет записей о доходах", font));
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
            addTableCell(incomesTable, record.getIin_bin() != null ? record.getIin_bin() : "N/A", font);
            addTableCell(incomesTable, record.getDate() != null ? record.getDate().toString() : "N/A", font);
            addTableCell(incomesTable, record.getDatabase() != null ? record.getDatabase() : "N/A", font);
            addTableCell(incomesTable, record.getOper() != null ? record.getOper() : "N/A", font);
            addTableCell(incomesTable, record.getDopinfo() != null ? record.getDopinfo() : "N/A", font);
            addTableCell(incomesTable, record.getSumm() != null ? record.getSumm() : "N/A", font);
        }

        document.add(incomesTable);
    }

    private void addTableHeader(PdfPTable table, String headerText, Font boldFont) {
        PdfPCell headerCell = new PdfPCell(new Phrase(headerText, boldFont));
        headerCell.setBackgroundColor(Color.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerCell.setPadding(8);
        table.addCell(headerCell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    public BaseFont getBaseFont() throws IOException, DocumentException {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/kztimesnewroman.ttf");
            try (InputStream fontStream = fontResource.getInputStream()) {
                byte[] fontBytes = fontStream.readAllBytes();
                return BaseFont.createFont("fonts/kztimesnewroman.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, fontBytes, null);
            }
        } catch (Exception e) {
            log.error("Error loading font: ", e);
            // Fallback to default font if loading fails
            return BaseFont.createFont();
        }
    }

    private static class WatermarkPageEvent extends PdfPageEventHelper {
        private final Font watermarkFont;

        public WatermarkPageEvent(Font font) {
            this.watermarkFont = font;
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
            } catch (Exception e) {
                log.error("Error adding watermark: ", e);
            }
        }
    }
}