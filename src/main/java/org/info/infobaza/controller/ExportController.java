package org.info.infobaza.controller;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.GetPortretRequest;
import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.service.export.ExcelExportService;
import org.info.infobaza.service.export.PdfExportService;
import org.info.infobaza.service.export.WordExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/export/")
@RequiredArgsConstructor
public class ExportController {
    private final WordExportService wordExportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @PostMapping("/pdf")
    public void exportToPdf(@RequestBody RelativesActiveRequest request,
                            HttpServletResponse response) throws IOException, DocumentException, NotFoundException {
        if (request.getIin() == null || request.getIin().isEmpty()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "IIN cannot be null or empty");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=person_" + request.getIin() + ".pdf");

        pdfExportService.exportToPdf(response.getOutputStream(), request);
    }

}
