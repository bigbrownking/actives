package org.info.infobaza.service.export;

import java.io.IOException;
import java.io.OutputStream;

public interface WordExportService {
    void exportToWord(OutputStream outputStream) throws IOException;
}
