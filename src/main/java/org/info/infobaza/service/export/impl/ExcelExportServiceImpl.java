
package org.info.infobaza.service.export.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.*;
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
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
    public void exportToExcel(OutputStream outputStream, RelativesActiveRequest request) throws IOException, NotFoundException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("Person Report");
            int rowIndex = 0;

            String dateFrom = request.getDateFrom().toString();
            String dateTo = request.getDateTo().toString();
            String iin = request.getIin();
            List<String> years = request.getYears() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYears();


            // Fetch data
            Person person = portretService.getPerson(iin);

            RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                    iin,
                    dateFrom, dateTo);

            RelationActiveWithTypes secondaryRelations = relationService.getSecondaryRelationsOfPerson(
                    iin,
                    dateFrom, dateTo);

            List<Pension> pensions = enpfService.getPension(
                    iin,
                    dateFrom, dateTo);

            Head head = headService.constructHead(
                    iin,
                    dateFrom, dateTo);

            Industry industry = industrialService.getIndustry(iin);

            ActiveWithRecords activeResponse = (ActiveWithRecords) analyzer.getAllActivesOfPersonsByDates(
                    iin,
                    dateFrom, dateTo,
                    years,
                    request.getVids(),
                    request.getTypes(),
                    request.getSources(),
                    request.getIins());

            IncomeWithRecords incomeResponse = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                    iin,
                    dateFrom, dateTo,
                    years,
                    request.getVids(),
                    request.getSources(),
                    request.getIins());

            // Add sections
            rowIndex = addPortraitSection(workbook, sheet, rowIndex, person);
            rowIndex = addRelationsSection(sheet, rowIndex, primaryRelations, secondaryRelations);
            rowIndex = addActivesAndIncomesSection(sheet, rowIndex, activeResponse, incomeResponse);
            rowIndex = addJobInformationSection(sheet, rowIndex, pensions, head, industry);

            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to output stream
            workbook.write(outputStream);
        } catch (NotFoundException e) {
            log.error("Data not found: ", e);
            throw e;
        } finally {
            workbook.close();
        }
    }

    private int addPortraitSection(XSSFWorkbook workbook, XSSFSheet sheet, int rowIndex, Person person) {
        rowIndex = addBoldRow(sheet, rowIndex, "Портрет");

        // Person info and image
        XSSFRow row = sheet.createRow(rowIndex++);
        XSSFCell imageCell = row.createCell(0);
        XSSFCell infoCell = row.createCell(1);

        // Add image
        if (person.getImage() != null && !person.getImage().isEmpty()) {
            try {
                String imageString = person.getImage();
                byte[] imageBytes;
                int pictureType = XSSFWorkbook.PICTURE_TYPE_JPEG;

                if (imageString.contains(";")) {
                    String[] imageParts = imageString.split(";");
                    String mimeType = imageParts[0].replace("data:", "");
                    String base64Data = imageParts[1].replace("base64,", "");
                    imageBytes = Base64.getDecoder().decode(base64Data);

                    if (mimeType.contains("png")) {
                        pictureType = XSSFWorkbook.PICTURE_TYPE_PNG;
                    } else if (!mimeType.contains("jpeg") && !mimeType.contains("jpg")) {
                        throw new IllegalArgumentException("Unsupported image format: " + mimeType);
                    }
                } else {
                    imageBytes = Base64.getDecoder().decode(imageString);
                }

                // Add image to workbook
                int pictureIdx = workbook.addPicture(imageBytes, pictureType);
                XSSFDrawing drawing = sheet.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, rowIndex - 1, 1, rowIndex);
                anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                XSSFPicture picture = drawing.createPicture(anchor, pictureIdx);
                picture.resize(100.0 / 914400.0, 100.0 / 914400.0); // 100 pixels (1 pixel ≈ 9144 EMUs)
            } catch (Exception e) {
                log.error("Failed to add image to Excel document: ", e);
                imageCell.setCellValue("");
            }
        } else {
            imageCell.setCellValue("");
        }

        // Person info
        StringBuilder infoText = new StringBuilder();
        infoText.append("ФИО: ").append(person.getFio() != null ? person.getFio() : "N/A").append("\n");
        infoText.append("Возраст: ").append(person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен").append("\n");
        infoText.append("ИИН: ").append(person.getIin() != null ? person.getIin() : "N/A").append("\n");
        infoText.append("Портрет: ").append(person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует").append("\n");
        infoText.append("Номинал: ").append(person.getIsNominal() != null && person.getIsNominal() ? "Номинал" : "Не номинал");
        infoCell.setCellValue(infoText.toString());

        return rowIndex + 1;
    }

    private int addRelationsSection(XSSFSheet sheet, int rowIndex, RelationActiveWithTypes primaryRelations,
                                    RelationActiveWithTypes secondaryRelations) {
        // Primary Relations
        rowIndex = addBoldRow(sheet, rowIndex, "Первичные связи:");
        if (primaryRelations != null && primaryRelations.getTypeToRelation() != null && !primaryRelations.getTypeToRelation().isEmpty()) {
            rowIndex = addRelationsTable(sheet, rowIndex, primaryRelations.getTypeToRelation());
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет первичных связей");
        }
        rowIndex++;

        // Secondary Relations
        rowIndex = addBoldRow(sheet, rowIndex, "Вторичные связи:");
        if (secondaryRelations != null && secondaryRelations.getTypeToRelation() != null && !secondaryRelations.getTypeToRelation().isEmpty()) {
            rowIndex = addRelationsTable(sheet, rowIndex, secondaryRelations.getTypeToRelation());
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет вторичных связей");
        }
        return rowIndex + 1;
    }

    private int addRelationsTable(XSSFSheet sheet, int rowIndex, Map<String, List<RelationActive>> relationsMap) {
        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            rowIndex = addBoldRow(sheet, rowIndex, category + ":");

            if (relations == null || relations.isEmpty()) {
                rowIndex = addRow(sheet, rowIndex, "  Нет связей в данной категории");
                rowIndex++;
                continue;
            }

            XSSFRow headerRow = sheet.createRow(rowIndex++);
            setCell(headerRow, 0, "ФИО", true);
            setCell(headerRow, 1, "Связь", true);
            setCell(headerRow, 2, "ИИН", true);
            setCell(headerRow, 3, "Активы", true);
            setCell(headerRow, 4, "Доходы", true);
            setCell(headerRow, 5, "Номинал", true);

            for (RelationActive ra : relations) {
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, ra.getFio() != null ? ra.getFio() : "N/A", false);
                setCell(row, 1, ra.getRelation() != null ? ra.getRelation() : "N/A", false);
                setCell(row, 2, ra.getIin() != null ? ra.getIin() : "N/A", false);
                setCell(row, 3, ra.getActives() != null ? ra.getActives() : "N/A", false);
                setCell(row, 4, ra.getIncomes() != null ? ra.getIncomes() : "N/A", false);
                setCell(row, 5, ra.isNominal() ? "Да": "Нет", false);
            }
        }
        return rowIndex;
    }

    private int addActivesAndIncomesSection(XSSFSheet sheet, int rowIndex, ActiveWithRecords activeResponse,
                                            IncomeWithRecords incomeResponse) {
        // Actives
        rowIndex = addBoldRow(sheet, rowIndex, "Активы:");
        if (activeResponse != null && activeResponse.getRecordsByOper() != null && !activeResponse.getRecordsByOper().isEmpty()) {
            rowIndex = addActivesTable(sheet, rowIndex, activeResponse.getRecordsByOper());
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет данных об активах");
        }
        rowIndex++;

        // Incomes
        rowIndex = addBoldRow(sheet, rowIndex, "Доходы:");
        if (incomeResponse != null && incomeResponse.getRecordsByYear() != null && !incomeResponse.getRecordsByYear().isEmpty()) {
            rowIndex = addIncomesTable(sheet, rowIndex, incomeResponse.getRecordsByYear());
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет данных о доходах");
        }
        return rowIndex + 1;
    }

    private int addActivesTable(XSSFSheet sheet, int rowIndex, Map<String, List<RecordDt>> recordsByOper) {
        for (Map.Entry<String, List<RecordDt>> entry : recordsByOper.entrySet()) {
            String operation = entry.getKey();
            List<RecordDt> records = entry.getValue();

            rowIndex = addBoldRow(sheet, rowIndex, operation + ":");

            if (records == null || records.isEmpty()) {
                rowIndex = addRow(sheet, rowIndex, "  Нет записей для данной операции");
                rowIndex++;
                continue;
            }

            boolean hasESFRecords = records.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

            // Header row
            XSSFRow headerRow = sheet.createRow(rowIndex++);
            if (hasESFRecords) {
                setCell(headerRow, 0, "ИИН/БИН", true);
                setCell(headerRow, 1, "ИИН Покуп.", true);
                setCell(headerRow, 2, "ИИН Прод.", true);
                setCell(headerRow, 3, "Дата", true);
                setCell(headerRow, 4, "База данных", true);
                setCell(headerRow, 5, "Активы", true);
                setCell(headerRow, 6, "Операция", true);
                setCell(headerRow, 7, "Доп. инфо", true);
                setCell(headerRow, 8, "Сумма", true);

                // Data rows
                for (RecordDt record : records) {
                    XSSFRow row = sheet.createRow(rowIndex++);
                    if (record instanceof ESFInformationRecordDt esfRecord) {
                        setCell(row, 0, esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "N/A", false);
                        setCell(row, 1, esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "N/A", false);
                        setCell(row, 2, esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "N/A", false);
                        setCell(row, 3, esfRecord.getDate() != null ? esfRecord.getDate().toString() : "N/A", false);
                        setCell(row, 4, esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "N/A", false);
                        setCell(row, 5, esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "N/A", false);
                        setCell(row, 6, esfRecord.getOper() != null ? esfRecord.getOper() : "N/A", false);
                        setCell(row, 7, esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "N/A", false);
                        setCell(row, 8, esfRecord.getSumm() != null ? esfRecord.getSumm() : "N/A", false);
                    } else {
                        setCell(row, 0, record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
                        setCell(row, 1, "N/A", false);
                        setCell(row, 2, "N/A", false);
                        setCell(row, 3, record.getDate() != null ? record.getDate().toString() : "N/A", false);
                        setCell(row, 4, record.getDatabase() != null ? record.getDatabase() : "N/A", false);
                        setCell(row, 5, "N/A", false);
                        setCell(row, 6, record.getOper() != null ? record.getOper() : "N/A", false);
                        setCell(row, 7, record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
                        setCell(row, 8, record.getSumm() != null ? record.getSumm() : "N/A", false);
                    }
                }
            } else {
                setCell(headerRow, 0, "ИИН/БИН", true);
                setCell(headerRow, 1, "Дата", true);
                setCell(headerRow, 2, "База данных", true);
                setCell(headerRow, 3, "Операция", true);
                setCell(headerRow, 4, "Доп. инфо", true);
                setCell(headerRow, 5, "Сумма", true);

                // Data rows
                for (RecordDt record : records) {
                    XSSFRow row = sheet.createRow(rowIndex++);
                    setCell(row, 0, record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
                    setCell(row, 1, record.getDate() != null ? record.getDate().toString() : "N/A", false);
                    setCell(row, 2, record.getDatabase() != null ? record.getDatabase() : "N/A", false);
                    setCell(row, 3, record.getOper() != null ? record.getOper() : "N/A", false);
                    setCell(row, 4, record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
                    setCell(row, 5, record.getSumm() != null ? record.getSumm() : "N/A", false);
                }
            }
        }
        return rowIndex;
    }

    private int addIncomesTable(XSSFSheet sheet, int rowIndex, List<RecordDt> records) {
        if (records == null || records.isEmpty()) {
            return addRow(sheet, rowIndex, "  Нет записей о доходах");
        }

        // Header row
        XSSFRow headerRow = sheet.createRow(rowIndex++);
        setCell(headerRow, 0, "ИИН/БИН", true);
        setCell(headerRow, 1, "Дата", true);
        setCell(headerRow, 2, "База данных", true);
        setCell(headerRow, 3, "Операция", true);
        setCell(headerRow, 4, "Доп. инфо", true);
        setCell(headerRow, 5, "Сумма", true);

        // Data rows
        for (RecordDt record : records) {
            XSSFRow row = sheet.createRow(rowIndex++);
            setCell(row, 0, record.getIin_bin() != null ? record.getIin_bin() : "N/A", false);
            setCell(row, 1, record.getDate() != null ? record.getDate().toString() : "N/A", false);
            setCell(row, 2, record.getDatabase() != null ? record.getDatabase() : "N/A", false);
            setCell(row, 3, record.getOper() != null ? record.getOper() : "N/A", false);
            setCell(row, 4, record.getDopinfo() != null ? record.getDopinfo() : "N/A", false);
            setCell(row, 5, record.getSumm() != null ? record.getSumm() : "N/A", false);
        }
        return rowIndex;
    }

    private int addJobInformationSection(XSSFSheet sheet, int rowIndex, List<Pension> pensions, Head head, Industry industry) {
        // Industry section
        rowIndex = addBoldRow(sheet, rowIndex, "Отрасль:");
        rowIndex = addRow(sheet, rowIndex, industry != null && industry.getName() != null && !industry.getName().isEmpty()
                ? industry.getName()
                : "Информация об отрасли отсутствует");
        rowIndex++;

        // Head section
        rowIndex = addBoldRow(sheet, rowIndex, "Руководящие позиции:");
        if (!head.isEmpty()) {
            rowIndex = addHeadInformationTable(sheet, rowIndex, head);
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет информации о руководящих позициях");
        }
        rowIndex++;

        // Pensions section
        rowIndex = addBoldRow(sheet, rowIndex, "Пенсионные взносы:");
        if (pensions != null && !pensions.isEmpty()) {
            rowIndex = addPensionsTable(sheet, rowIndex, pensions);
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет данных о пенсионных взносах");
        }
        return rowIndex;
    }

    private int addHeadInformationTable(XSSFSheet sheet, int rowIndex, Head head) {
        // Supervisor information
        if (head.getHead() != null && !head.getHead().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "Руководящие должности:");
            XSSFRow headerRow = sheet.createRow(rowIndex++);
            setCell(headerRow, 0, "ИИН/БИН", true);
            setCell(headerRow, 1, "Тип позиции", true);
            setCell(headerRow, 2, "ИИН/БИН налогоплательщика", true);
            setCell(headerRow, 3, "Тип", true);
            setCell(headerRow, 4, "Наименование", true);

            for (SupervisorRecord supervisor : head.getHead()) {
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "N/A", false);
                setCell(row, 1, supervisor.getPositionType() != null ? supervisor.getPositionType() : "N/A", false);
                setCell(row, 2, supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "N/A", false);
                setCell(row, 3, supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "N/A", false);
                setCell(row, 4, supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "N/A", false);
            }
        }

        // Company information
        if (head.getOked() != null && !head.getOked().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "Компании:");
            XSSFRow headerRow = sheet.createRow(rowIndex++);
            setCell(headerRow, 0, "Русское название", true);
            setCell(headerRow, 1, "Оригинальное название", true);
            setCell(headerRow, 2, "БИН", true);
            setCell(headerRow, 3, "Дата регистрации", true);
            setCell(headerRow, 4, "Телефон", true);

            for (CompanyRecord company : head.getOked()) {
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, company.getRusName() != null ? company.getRusName() : "N/A", false);
                setCell(row, 1, company.getOrigName() != null ? company.getOrigName() : "N/A", false);
                setCell(row, 2, company.getBin() != null ? company.getBin() : "N/A", false);
                setCell(row, 3, company.getDateReg() != null ? company.getDateReg().toString() : "N/A", false);
                setCell(row, 4, company.getTelephone() != null ? company.getTelephone() : "N/A", false);
            }
        }

        // Financial summary
        rowIndex = addBoldRow(sheet, rowIndex, "Финансовая информация:");
        rowIndex = addRow(sheet, rowIndex, "Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "N/A"));
        rowIndex = addRow(sheet, rowIndex, "Налоги: " + (head.getTax() != null ? head.getTax().toString() : "N/A"));

        // ESF information
        if (head.getEsf() != null && !head.getEsf().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "ESF информация:");
            XSSFRow headerRow = sheet.createRow(rowIndex++);
            setCell(headerRow, 0, "ИИН/БИН", true);
            setCell(headerRow, 1, "Дата", true);
            setCell(headerRow, 2, "Активы", true);
            setCell(headerRow, 3, "Сумма", true);

            for (org.info.infobaza.model.info.active_income.EsfOverall esf : head.getEsf()) {
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, esf.getIin_bin() != null ? esf.getIin_bin() : "N/A", false);
                setCell(row, 1, esf.getDate() != null ? esf.getDate().toString() : "N/A", false);
                setCell(row, 2, esf.getAktivy() != null ? esf.getAktivy() : "N/A", false);
                setCell(row, 3, esf.getSumm() != null ? esf.getSumm().toString() : "N/A", false);
            }
        }

        // Statuses
        if (head.getStatuses() != null && !head.getStatuses().isEmpty()) {
            rowIndex = addBoldRow(sheet, rowIndex, "Статусы:");
            rowIndex = addRow(sheet, rowIndex, String.join(", ", head.getStatuses()));
        }
        return rowIndex;
    }

    private int addPensionsTable(XSSFSheet sheet, int rowIndex, List<Pension> pensions) {
        XSSFRow headerRow = sheet.createRow(rowIndex++);
        setCell(headerRow, 0, "Дата с", true);
        setCell(headerRow, 1, "Дата по", true);
        setCell(headerRow, 2, "Наименование", true);
        setCell(headerRow, 3, "P_RNN", true);

        for (Pension pension : pensions) {
            XSSFRow row = sheet.createRow(rowIndex++);
            setCell(row, 0, pension.getDateFrom() != null ? pension.getDateFrom() : "N/A", false);
            setCell(row, 1, pension.getDateTo() != null ? pension.getDateTo() : "N/A", false);
            setCell(row, 2, pension.getName() != null ? pension.getName() : "N/A", false);
            setCell(row, 3, pension.getP_RNN() != null ? pension.getP_RNN() : "N/A", false);
        }
        return rowIndex;
    }

    private int addBoldRow(XSSFSheet sheet, int rowIndex, String text) {
        XSSFRow row = sheet.createRow(rowIndex++);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(text);
        XSSFCellStyle style = sheet.getWorkbook().createCellStyle();
        XSSFFont font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        cell.setCellStyle(style);
        return rowIndex;
    }

    private int addRow(XSSFSheet sheet, int rowIndex, String text) {
        XSSFRow row = sheet.createRow(rowIndex++);
        setCell(row, 0, text, false);
        return rowIndex;
    }

    private void setCell(XSSFRow row, int colIndex, String text, boolean isBold) {
        XSSFCell cell = row.createCell(colIndex);
        cell.setCellValue(text);
        XSSFCellStyle style = row.getSheet().getWorkbook().createCellStyle();
        XSSFFont font = row.getSheet().getWorkbook().createFont();
        font.setBold(isBold);
        style.setFont(font);
        cell.setCellStyle(style);
    }
}