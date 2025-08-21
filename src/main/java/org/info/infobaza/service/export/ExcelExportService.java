package org.info.infobaza.service.export;

import java.io.IOException;
import java.io.OutputStream;

public interface ExcelExportService {
    void exportToExcel(OutputStream outputStream) throws IOException;
}
