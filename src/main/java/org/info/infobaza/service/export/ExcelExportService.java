package org.info.infobaza.service.export;

import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.exception.NotFoundException;

import java.io.IOException;
import java.io.OutputStream;

public interface ExcelExportService {
    void exportToExcel(OutputStream outputStream, RelativesActiveRequest request) throws IOException, NotFoundException;
}
