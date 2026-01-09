package org.info.infobaza.controller;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.request.MassExportRequest;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.security.UserDetailsImpl;
import org.info.infobaza.service.export.ExcelExportService;
import org.info.infobaza.service.export.PdfExportService;
import org.info.infobaza.service.export.WordExportService;
import org.info.infobaza.util.logging.JustificationService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.info.infobaza.constants.Dictionary.*;

@RestController
@RequestMapping("/export/")
@RequiredArgsConstructor
public class ExportController {
    private final WordExportService wordExportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final JustificationService justificationService;


    @LogRequest
    @PostMapping("/pdf")
    public void exportToPdf(@RequestBody ExportRequest exportRequest,
                            HttpServletResponse response) throws IOException, DocumentException, NotFoundException {
        generate_pdf_header(exportRequest, response);
        pdfExportService.exportToPdf(response.getOutputStream(), exportRequest);
    }

    @LogRequest
    @PostMapping("/word")
    public void exportToWord(@RequestBody ExportRequest exportRequest,
                             HttpServletResponse response) throws IOException, NotFoundException {
        generate_word_header(exportRequest, response);
        wordExportService.exportToWord(response.getOutputStream(), exportRequest);
    }

    @LogRequest
    @PostMapping("/excel")
    public void exportToExcel(@RequestBody ExportRequest exportRequest,
                              HttpServletResponse response) throws IOException, NotFoundException {
        generate_excel_header(exportRequest, response);
        excelExportService.exportToExcel(response.getOutputStream(), exportRequest);
    }

    @LogRequest
    @PostMapping("/mass")
    public void massExport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody MassExportRequest massExportRequest,
                           HttpServletResponse response) throws IOException, NotFoundException {
        if(justificationService.store(
                userDetails,
                massExportRequest.getIins(),
                massExportRequest.getOrderNum(),
                massExportRequest.getApprovement_type(),
                massExportRequest.getCaseNum(),
                massExportRequest.getOrderDate(),
                massExportRequest.getArticleName(),
                massExportRequest.getCheckingName(),
                massExportRequest.getOtherReasons(),
                massExportRequest.getOrganName(),
                massExportRequest.getSphereName(),
                massExportRequest.getTematikName(),
                massExportRequest.getRukName(),
                "Выгрузка профилей по иин: "
        )) {
            generate_excel_header(massExportRequest, response);
            excelExportService.exportToExcelMass(response.getOutputStream(), massExportRequest);
        }
    }
}
