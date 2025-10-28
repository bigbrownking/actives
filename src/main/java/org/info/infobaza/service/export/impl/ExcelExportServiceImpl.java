
package org.info.infobaza.service.export.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.*;
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
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.model.info.active_income.RecordDt;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.PenaltyRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.adm_shtraf.AdministrationPayService;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private final PortretService portretService;
    private final RelationService relationService;
    private final HeadService headService;
    private final IndustrialService industrialService;
    private final AdministrationPayService administrationPayService;
    private final ENPFService enpfService;
    private final Analyzer analyzer;
    private final DateUtil dateUtil;

    @Override
    public void exportToExcel(OutputStream outputStream, ExportRequest request) throws IOException, NotFoundException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Person Report");
            int rowIndex = 0;

            String dateFrom = request.getDateFrom().toString();
            String dateTo = request.getDateTo().toString();
            String mainIin = request.getIin();
            List<String> yearsActive = request.getYearsActive() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsActive();
            List<String> yearsIncome = request.getYearsIncome() == null ? dateUtil.getYears(dateFrom, dateTo) : request.getYearsIncome();

            List<String> iinsToProcess = new ArrayList<>();
            iinsToProcess.add(mainIin);

            // Fetch primary relations for the main IIN
            RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                    mainIin, dateFrom, dateTo);
            if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                iinsToProcess.addAll(
                        primaryRelations.getTypeToRelation().values().stream()
                                .flatMap(List::stream)
                                .map(RelationActive::getIin)
                                .filter(x -> x != null && !x.isEmpty())
                                .distinct()
                                .toList()
                );
            }

            // Process each IIN
            for (int i = 0; i < iinsToProcess.size(); i++) {
                String iin = iinsToProcess.get(i);

                // Add section header for the current IIN
                rowIndex = addBoldRow(sheet, rowIndex, "Данные для ИИН: " + iin);
                rowIndex++;

                // Fetch data for the current IIN
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
                rowIndex = addPortraitSection(workbook, sheet, rowIndex, person);
                rowIndex = addRelationsSection(sheet, rowIndex, personPrimaryRelations, personSecondaryRelations);
                rowIndex = addActivesAndIncomesSection(sheet, rowIndex, activeResponse, incomeResponse);
                rowIndex = addJobInformationSection(sheet, rowIndex, pensions, head, industry, turnoverRecords);

                // Add blank row between IINs (except for the last one)
                if (i < iinsToProcess.size() - 1) {
                    rowIndex++;
                }
            }

            // Auto-size columns
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to output stream
            workbook.write(outputStream);
        }

    }

    @Override
    public void exportToExcelMass(OutputStream outputStream, MassExportRequest request) throws IOException, NotFoundException {
        List<String> iins = request.getIins();
        String dateFrom = "1980";
        String dateTo = "2025";
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Person Report");
            int rowIndex = 0;
            for (String iinInput : iins) {
                List<String> iinsToProcess = new ArrayList<>();
                iinsToProcess.add(iinInput);

                // Fetch primary relations for the main IIN
                RelationActiveWithTypes primaryRelations = relationService.getPrimaryRelationsOfPerson(
                        iinInput, dateFrom, dateTo);
                if (primaryRelations != null && primaryRelations.getTypeToRelation() != null) {
                    iinsToProcess.addAll(
                            primaryRelations.getTypeToRelation().values().stream()
                                    .flatMap(List::stream)
                                    .map(RelationActive::getIin)
                                    .filter(x -> x != null && !x.isEmpty())
                                    .distinct()
                                    .toList()
                    );
                }

                // Process each IIN
                for (int i = 0; i < iinsToProcess.size(); i++) {
                    String iin = iinsToProcess.get(i);

                    // Add section header for the current IIN
                    rowIndex = addBoldRow(sheet, rowIndex, "Данные для ИИН: " + iin);
                    rowIndex++;

                    // Fetch data for the current IIN
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
                            iin, dateFrom, dateTo, null, null, null,
                            null, request.getIins());
                    IncomeWithRecords incomeResponse = (IncomeWithRecords) analyzer.getAllIncomesOfPersonsByDates(
                            iin, dateFrom, dateTo, null, null, null,
                            request.getIins());

                    // Add sections for this IIN
                    rowIndex = addPortraitSection(workbook, sheet, rowIndex, person);
                    rowIndex = addRelationsSection(sheet, rowIndex, personPrimaryRelations, personSecondaryRelations);
                    rowIndex = addActivesAndIncomesSection(sheet, rowIndex, activeResponse, incomeResponse);
                    rowIndex = addJobInformationSection(sheet, rowIndex, pensions, head, industry, turnoverRecords);

                    // Add blank row between IINs (except for the last one)
                    if (i < iinsToProcess.size() - 1) {
                        rowIndex++;
                    }
                }

                // Auto-size columns
                for (int i = 0; i < 9; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Write to output stream
                workbook.write(outputStream);
            }
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
        infoText.append("ФИО: ").append(person.getFio() != null ? person.getFio() : "-").append("\n");
        infoText.append("Возраст: ").append(person.getAge() != 0 ? person.getAge() + " лет" : "Возраст неизвестен").append("\n");
        infoText.append("ИИН: ").append(person.getIin() != null ? person.getIin() : "-").append("\n");
        infoText.append("Портрет: ").append(person.getPortret() != null ? String.join(", ", person.getPortret()) : "Портрет отсутствует").append("\n");
        infoText.append("Номинал: ").append(person.getIsNominal() != null && person.getIsNominal() ? "Номинал" : "Не номинал");
        infoText.append("Крипта: ").append(person.getIsCryptoActive() ? "Есть переводы по криптовалюте" : "Нету переводов по криптовалюте");
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
        log.debug("Adding relations table to Excel sheet");

        for (Map.Entry<String, List<RelationActive>> entry : relationsMap.entrySet()) {
            String category = entry.getKey();
            List<RelationActive> relations = entry.getValue();

            rowIndex = addBoldRow(sheet, rowIndex, category + ":");

            if (relations == null || relations.isEmpty()) {
                rowIndex = addRow(sheet, rowIndex, "  Нет связей в данной категории");
                rowIndex++;
                log.debug("No relations in category: {}", category);
                continue;
            }

            XSSFRow headerRow = sheet.createRow(rowIndex++);
            setCell(headerRow, 0, "ФИО", true);
            setCell(headerRow, 1, "Связь", true);
            setCell(headerRow, 2, "ИИН", true);
            setCell(headerRow, 3, "Активы", true);
            setCell(headerRow, 4, "Доходы", true);
            setCell(headerRow, 5, "Номинал", true);
            setCell(headerRow, 6, "Доп Инфо", true);

            for (RelationActive ra : relations) {
                log.debug("Processing RelationActive for IIN: {}, dopinfo: {}", ra.getIin(), ra.getDopinfo());
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, ra.getFio() != null ? ra.getFio() : "-", false);
                setCell(row, 1, ra.getRelation() != null ? ra.getRelation() : "-", false);
                setCell(row, 2, ra.getIin() != null ? ra.getIin() : "-", false);
                setCell(row, 3, ra.getActives() != null ? ra.getActives() : "-", false);
                setCell(row, 4, ra.getIncomes() != null ? ra.getIncomes() : "-", false);
                setCell(row, 5, ra.isNominal() ? "Да" : "Нет", false);

                // Format dopinfo as a readable string
                String dopinfoStr = "-";
                if (ra.getDopinfo() != null && !ra.getDopinfo().isEmpty()) {
                    dopinfoStr = ra.getDopinfo().entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("; "));
                }
                setCell(row, 6, dopinfoStr, false);
            }
            log.debug("Added relations table for category: {}", category);
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

    private int addActivesTable(XSSFSheet sheet, int rowIndex, List<RecordDt> recordsByOper) {
        rowIndex = addBoldRow(sheet, rowIndex, "Активы:");

        if (recordsByOper == null || recordsByOper.isEmpty()) {
            rowIndex = addRow(sheet, rowIndex, "  Нет записей для активов");
            rowIndex++;
            return rowIndex;
        }

        boolean hasESFRecords = recordsByOper.stream().anyMatch(record -> record instanceof ESFInformationRecordDt);

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
            for (RecordDt record : recordsByOper) {
                XSSFRow row = sheet.createRow(rowIndex++);
                if (record instanceof ESFInformationRecordDt esfRecord) {
                    setCell(row, 0, esfRecord.getIin_bin() != null ? esfRecord.getIin_bin() : "-", false);
                    setCell(row, 1, esfRecord.getIin_bin_pokup() != null ? esfRecord.getIin_bin_pokup() : "-", false);
                    setCell(row, 2, esfRecord.getIin_bin_prod() != null ? esfRecord.getIin_bin_prod() : "-", false);
                    setCell(row, 3, esfRecord.getDate() != null ? esfRecord.getDate().toString() : "-", false);
                    setCell(row, 4, esfRecord.getDatabase() != null ? esfRecord.getDatabase() : "-", false);
                    setCell(row, 5, esfRecord.getAktivy() != null ? esfRecord.getAktivy() : "-", false);
                    setCell(row, 6, esfRecord.getOper() != null ? esfRecord.getOper() : "-", false);
                    setCell(row, 7, esfRecord.getDopinfo() != null ? esfRecord.getDopinfo() : "-", false);
                    setCell(row, 8, esfRecord.getSumm() != null ? esfRecord.getSumm() : "-", false);
                } else if (record instanceof InformationRecordDt info) {
                    setCell(row, 0, info.getIin_bin() != null ? record.getIin_bin() : "-", false);
                    setCell(row, 1, "-", false);
                    setCell(row, 2, "-", false);
                    setCell(row, 3, info.getDate() != null ? record.getDate().toString() : "-", false);
                    setCell(row, 4, info.getDatabase() != null ? record.getDatabase() : "-", false);
                    setCell(row, 5, info.getAktivy() != null ? record.getAktivy() : "-", false);
                    setCell(row, 6, info.getOper() != null ? record.getOper() : "-", false);
                    setCell(row, 7, info.getDopinfo() != null ? record.getDopinfo() : "-", false);
                    setCell(row, 8, info.getSumm() != null ? record.getSumm() : "-", false);
                } else if (record instanceof NaoConRecordDt nao) {
                    setCell(row, 0, nao.getIin_bin() != null ? record.getIin_bin() : "-", false);
                    setCell(row, 1, "-", false);
                    setCell(row, 2, "-", false);
                    setCell(row, 3, nao.getDate() != null ? record.getDate().toString() : "-", false);
                    setCell(row, 4, nao.getDatabase() != null ? record.getDatabase() : "-", false);
                    setCell(row, 5, nao.getAktivy() != null ? record.getAktivy() : "-", false);
                    setCell(row, 6, nao.getOper() != null ? record.getOper() : "-", false);
                    setCell(row, 7, nao.getDopinfo() != null ? record.getDopinfo() : "-", false);
                    setCell(row, 8, nao.getSumm() != null ? record.getSumm() : "-", false);
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
            for (RecordDt record : recordsByOper) {
                XSSFRow row = sheet.createRow(rowIndex++);
                setCell(row, 0, record.getIin_bin() != null ? record.getIin_bin() : "-", false);
                setCell(row, 1, record.getDate() != null ? record.getDate().toString() : "-", false);
                setCell(row, 2, record.getDatabase() != null ? record.getDatabase() : "-", false);
                setCell(row, 3, record.getOper() != null ? record.getOper() : "-", false);
                setCell(row, 4, record.getDopinfo() != null ? record.getDopinfo() : "-", false);
                setCell(row, 5, record.getSumm() != null ? record.getSumm() : "-", false);
            }
        }

        rowIndex++;
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
            setCell(row, 0, record.getIin_bin() != null ? record.getIin_bin() : "-", false);
            setCell(row, 1, record.getDate() != null ? record.getDate().toString() : "-", false);
            setCell(row, 2, record.getDatabase() != null ? record.getDatabase() : "-", false);
            setCell(row, 3, record.getOper() != null ? record.getOper() : "-", false);
            setCell(row, 4, record.getDopinfo() != null ? record.getDopinfo() : "-", false);
            setCell(row, 5, record.getSumm() != null ? record.getSumm() : "", false);
        }
        return rowIndex;
    }

    private int addJobInformationSection(XSSFSheet sheet, int rowIndex,
                                         List<Pension> pensions, Head head,
                                         Industry industry, List<TurnoverRecord> turnoverRecords) {
        // Industry section
        rowIndex = addBoldRow(sheet, rowIndex, "Отрасль:");
        rowIndex = addRow(sheet, rowIndex, industry != null && industry.getName() != null && !industry.getName().isEmpty()
                ? industry.getName()
                : "Информация об отрасли отсутствует");
        rowIndex++;


        // Pensions section
        rowIndex = addBoldRow(sheet, rowIndex, "Пенсионные взносы:");
        if (pensions != null && !pensions.isEmpty()) {
            rowIndex = addPensionsTable(sheet, rowIndex, pensions);
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет данных о пенсионных взносах");
        }


        // Head section
        rowIndex = addBoldRow(sheet, rowIndex, "Руководящие позиции:");
        if (!head.isEmpty()) {
            rowIndex = addHeadInformationTable(sheet, rowIndex, head);
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет информации о руководящих позициях");
        }
        rowIndex++;


        // Turnover section
        rowIndex = addBoldRow(sheet, rowIndex, "Банковские счета:");
        if (turnoverRecords != null && !turnoverRecords.isEmpty()) {
            rowIndex = addTurnoversTable(sheet, rowIndex, turnoverRecords);
        } else {
            rowIndex = addRow(sheet, rowIndex, "Нет информации о банковских счетах");
        }
        return rowIndex + 1;
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
                setCell(row, 0, supervisor.getIin_bin() != null ? supervisor.getIin_bin() : "-", false);
                setCell(row, 1, supervisor.getPositionType() != null ? supervisor.getPositionType() : "-", false);
                setCell(row, 2, supervisor.getTaxpayer_iin_bin() != null ? supervisor.getTaxpayer_iin_bin() : "-", false);
                //  setCell(row, 3, supervisor.getTaxpayerType() != null ? supervisor.getTaxpayerType() : "-", false);
                setCell(row, 4, supervisor.getTaxpayerName() != null ? supervisor.getTaxpayerName() : "-", false);
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
                setCell(row, 0, company.getRusName() != null ? company.getRusName() : "-", false);
                setCell(row, 1, company.getOrigName() != null ? company.getOrigName() : "-", false);
                setCell(row, 2, company.getBin() != null ? company.getBin() : "-", false);
                setCell(row, 3, company.getDateReg() != null ? company.getDateReg().toString() : "-", false);
                setCell(row, 4, company.getTelephone() != null ? company.getTelephone() : "-", false);
            }
        }

        // Financial summary
        rowIndex = addBoldRow(sheet, rowIndex, "Финансовая информация:");
        rowIndex = addRow(sheet, rowIndex, "Доход: " + (head.getIncome() != null ? head.getIncome().toString() : "-"));
        rowIndex = addRow(sheet, rowIndex, "Налоги: " + (head.getTax() != null ? head.getTax().toString() : "-"));

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
                setCell(row, 0, esf.getIin_bin() != null ? esf.getIin_bin() : "-", false);
                setCell(row, 1, esf.getDate() != null ? esf.getDate().toString() : "-", false);
                setCell(row, 2, esf.getAktivy() != null ? esf.getAktivy() : "-", false);
                setCell(row, 3, esf.getSumm() != null ? esf.getSumm().toString() : "-", false);
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
        setCell(headerRow, 4, "Максимальная з.п.", true);
        setCell(headerRow, 5, "Последняя з.п.", true);
        setCell(headerRow, 6, "Суммарно", true);

        for (Pension pension : pensions) {
            XSSFRow row = sheet.createRow(rowIndex++);
            setCell(row, 0, pension.getDateFrom() != null ? pension.getDateFrom() : "-", false);
            setCell(row, 1, pension.getDateTo() != null ? pension.getDateTo() : "-", false);
            setCell(row, 2, pension.getName() != null ? pension.getName() : "-", false);
            setCell(row, 3, pension.getP_RNN() != null ? pension.getP_RNN() : "-", false);
            setCell(row, 4, pension.getMaxSalary() != null ? pension.getMaxSalary() : "-", false);
            setCell(row, 5, pension.getLastSalary() != null ? pension.getLastSalary() : "-", false);
            setCell(row, 6, pension.getSumm() != null ? pension.getSumm() : "-", false);
        }
        return rowIndex;
    }

    private int addTurnoversTable(XSSFSheet sheet, int rowIndex,
                                  List<TurnoverRecord> turnoverRecords) {
        XSSFRow headerRow = sheet.createRow(rowIndex++);
        setCell(headerRow, 0, "ИИН/БИН", true);
        setCell(headerRow, 1, "Название банка", true);
        setCell(headerRow, 2, "Счет", true);
        setCell(headerRow, 3, "Сумма", true);
        setCell(headerRow, 4, "Дата от", true);
        setCell(headerRow, 5, "Дата до", true);
        setCell(headerRow, 6, "Источник", true);

        for (TurnoverRecord turnoverRecord : turnoverRecords) {
            XSSFRow row = sheet.createRow(rowIndex++);
            setCell(row, 0, turnoverRecord.getIinBin() != null ? turnoverRecord.getIinBin() : "-", false);
            setCell(row, 1, turnoverRecord.getBankName() != null ? turnoverRecord.getBankName() : "-", false);
            setCell(row, 2, turnoverRecord.getBankAccount() != null ? turnoverRecord.getBankAccount() : "-", false);
            setCell(row, 3, turnoverRecord.getSumm() != null ? turnoverRecord.getSumm() : "-", false);
            setCell(row, 4, turnoverRecord.getStartDate() != null ? turnoverRecord.getStartDate() : "-", false);
            setCell(row, 5, turnoverRecord.getEndDate() != null ? turnoverRecord.getEndDate() : "-", false);
            setCell(row, 6, turnoverRecord.getSource() != null ? turnoverRecord.getSource() : "-", false);
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