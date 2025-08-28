package org.info.infobaza.controller;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.service.export.ExcelExportService;
import org.info.infobaza.service.export.PdfExportService;
import org.info.infobaza.service.export.WordExportService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.info.infobaza.constants.Dictionary.*;

@RestController
@RequestMapping("/export/")
@RequiredArgsConstructor
public class ExportController {
    private final WordExportService wordExportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

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
}
