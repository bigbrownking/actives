package org.info.infobaza.service;

import java.io.IOException;
import java.util.List;

public interface DataFetcher {
    List<? extends Object> fetchData(String iin, String dateFrom, String dateTo) throws IOException;
    FetcherMetadata getMetadata();
    default List<?> filterRecords(List<?> records, String currentIin, String mainIin, boolean isUl) {
        return records;
    }
}
