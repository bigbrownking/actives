package org.info.infobaza.service.export;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.info.infobaza.dto.request.ExportRequest;
import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.exception.NotFoundException;

import java.io.IOException;
import java.io.OutputStream;

public interface PdfExportService {
    void exportToPdf(OutputStream outputStream, ExportRequest request) throws IOException, NotFoundException, DocumentException;
}
