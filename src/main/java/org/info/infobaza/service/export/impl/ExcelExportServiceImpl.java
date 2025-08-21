package org.info.infobaza.service.export.impl;

import org.info.infobaza.service.export.ExcelExportService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {
    @Override
    public void exportToExcel(OutputStream outputStream) throws IOException {

    }
}
