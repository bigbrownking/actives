package org.info.infobaza.util.convert;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SQLFileUtil {
    private final Map<String, String> sqlCache = new HashMap<>();
    private final ResourceLoader resourceLoader;

    public SQLFileUtil(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public synchronized String getSqlFromFile(String filePath) throws IOException {
        if (sqlCache.containsKey(filePath)) {
            return sqlCache.get(filePath);
        }

        try {
            Resource resource = resourceLoader.getResource("classpath:" + filePath);
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            sqlCache.put(filePath, sql);
            return sql;
        } catch (IOException e) {
            throw new IOException("Failed to load SQL file: " + filePath, e);
        }
    }

    public String getSqlWithIin(String filePath, String iin) throws IOException {
        String sql = getSqlFromFile(filePath);
        return sql.replace("$P-IIN", iin);
    }

    public String getSqlWithIinAndDates(String filePath, String iin, String dateFrom, String dateTo) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("$P-IIN", iin);
        params.put("$P-DATEFROM", dateFrom);
        params.put("$P-DATETO", dateTo);
        return getSqlWithParams(filePath, params);
    }
    private String getSqlWithParams(String filePath, Map<String, String> params) throws IOException {
        String sql = getSqlFromFile(filePath);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sql = sql.replace(entry.getKey(), entry.getValue());
        }
        return sql;
    }
}