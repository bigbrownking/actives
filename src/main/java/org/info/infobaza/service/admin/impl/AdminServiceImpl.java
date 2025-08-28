package org.info.infobaza.service.admin.impl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.request.LogSearchRequest;
import org.info.infobaza.model.info.log.Logs;
import org.info.infobaza.service.admin.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.info.infobaza.util.convert.JpaPageable.createPageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    @Value("${app.logs.base-path}")
    private String logsBasePath;

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+([\\w\\.]+)\\s+-\\s+(.*)"
    );

    private static final Pattern OLD_REQUEST_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) - Request: Controller: (\\w+), Method: (\\w+), HTTP: (\\w+), URI: ([^,]+), Args: \\[([^\\]]+)\\], ExecutionTime: (\\d+)ms"
    );

    private static final Pattern REQUEST_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) - Request: Controller: (\\w+), Method: (\\w+), HTTP: (\\w+), URI: ([^,]+), ClientIP: ([^,]+), Args: \\[([^\\]]+)\\], ExecutionTime: (\\d+)ms"
    );

    @Override
    public Page<Logs> searchFromLogs(LogSearchRequest request, int page, int size) {
        List<Logs> allLogs = new ArrayList<>();

        List<Path> filesToSearch = getFilesToSearch(request);

        for (Path filePath : filesToSearch) {
            List<Logs> fileLogs = parseLogFile(filePath);
            List<Logs> filteredLogs = filterLogsByIinBin(fileLogs, request.getIin_bin());
            allLogs.addAll(filteredLogs);
        }

        allLogs.sort(Comparator.comparing(Logs::getTimestamp).reversed());

        Pageable pageable = createPageable(page, size);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allLogs.size());

        List<Logs> pageContent = start >= allLogs.size() ? new ArrayList<>() : allLogs.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allLogs.size());
    }

    private List<Path> getFilesToSearch(LogSearchRequest request) {
        List<Path> filesToSearch = new ArrayList<>();
        Path basePath = Paths.get(logsBasePath);

        if (request.getDateFrom() == null && request.getDateTo() == null) {
            Path activeLog = basePath.resolve("active.log");
            if (Files.exists(activeLog)) {
                filesToSearch.add(activeLog);
            }

            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Path currentMonthPath = basePath.resolve(currentMonth);
            if (Files.exists(currentMonthPath)) {
                addFilesFromDirectory(currentMonthPath, filesToSearch);
            }
        } else {
            LocalDate startDate = request.getDateFrom() != null ? request.getDateFrom() : LocalDate.now().minusDays(30);
            LocalDate endDate = request.getDateTo() != null ? request.getDateTo() : LocalDate.now();

            if (endDate.equals(LocalDate.now())) {
                Path activeLog = basePath.resolve("active.log");
                if (Files.exists(activeLog)) {
                    filesToSearch.add(activeLog);
                }
            }

            Set<String> monthsToSearch = getMonthsInRange(startDate, endDate);

            for (String month : monthsToSearch) {
                Path monthPath = basePath.resolve(month);
                if (Files.exists(monthPath)) {
                    addFilesFromDirectory(monthPath, filesToSearch, startDate, endDate);
                }
            }
        }

        return filesToSearch;
    }

    private void addFilesFromDirectory(Path directory, List<Path> filesToSearch) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .forEach(filesToSearch::add);
        } catch (IOException e) {
            log.error("Error reading directory: " + directory, e);
        }
    }

    private void addFilesFromDirectory(Path directory, List<Path> filesToSearch, LocalDate startDate, LocalDate endDate) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .filter(path -> {
                        LocalDate fileDate = getDateFromFileName(path.getFileName().toString());
                        return fileDate != null &&
                                !fileDate.isBefore(startDate) &&
                                !fileDate.isAfter(endDate);
                    })
                    .forEach(filesToSearch::add);
        } catch (IOException e) {
            log.error("Error reading directory: " + directory, e);
        }
    }

    private Set<String> getMonthsInRange(LocalDate startDate, LocalDate endDate) {
        Set<String> months = new HashSet<>();
        LocalDate current = startDate.withDayOfMonth(1);

        while (!current.isAfter(endDate)) {
            months.add(current.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            current = current.plusMonths(1);
        }

        return months;
    }

    private List<Logs> parseLogFile(Path filePath) {
        List<Logs> logs = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                Logs logEntry = parseLogLine(line, filePath.getFileName().toString(), lineNumber);
                if (logEntry != null) {
                    logs.add(logEntry);
                }
            }
        } catch (IOException e) {
            log.error("Error reading log file: " + filePath, e);
        }

        return logs;
    }

    private Logs parseLogLine(String line, String fileName, long lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            log.debug("Skipping empty line {} in file {}", lineNumber, fileName);
            return null;
        }

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.find()) {
            log.debug("Line does not match LOG_PATTERN: {} (file: {}, line: {})", line, fileName, lineNumber);
            return null;
        }

        try {
            LocalDateTime outerTimestamp = LocalDateTime.parse(matcher.group(1),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            String message = matcher.group(4).trim();

            Logs.LogsBuilder logBuilder = Logs.builder()
                    .fileName(fileName)
                    .timestamp(outerTimestamp)
                    .lineNumber(lineNumber);

            // Try parsing as current log format
            Matcher requestMatcher = REQUEST_PATTERN.matcher(message);
            if (requestMatcher.find()) {
                LocalDateTime innerTimestamp = LocalDateTime.parse(requestMatcher.group(1),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

                logBuilder.timestamp(innerTimestamp)
                        .clientIp(requestMatcher.group(6))
                        .requestArgs(parseArguments(requestMatcher.group(7)))
                        .executionTimeMs(Long.parseLong(requestMatcher.group(8)));

                return logBuilder.build();
            }

            // Try parsing as old log format
            Matcher oldRequestMatcher = OLD_REQUEST_PATTERN.matcher(message);
            if (oldRequestMatcher.find()) {
                LocalDateTime innerTimestamp = LocalDateTime.parse(oldRequestMatcher.group(1),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

                logBuilder.timestamp(innerTimestamp)
                        .requestArgs(parseArguments(oldRequestMatcher.group(6)))
                        .executionTimeMs(Long.parseLong(oldRequestMatcher.group(7)));

                return logBuilder.build();
            }

            log.debug("Line does not match request patterns: {} (file: {}, line: {})", line, fileName, lineNumber);
            return null;

        } catch (DateTimeParseException e) {
            log.warn("Failed to parse timestamp in line {} of file {}: {}", lineNumber, fileName, line, e);
            return null;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse execution time in line {} of file {}: {}", lineNumber, fileName, line, e);
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error parsing line {} of file {}: {}", lineNumber, fileName, line, e);
            return null;
        }
    }

    private Map<String, Object> parseArguments(String argsStr) {
        Map<String, Object> args = new HashMap<>();
        if (argsStr == null || argsStr.trim().isEmpty()) {
            return args;
        }

        try {
            String[] pairs = argsStr.split(",\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                    args.put(key, value);
                } else {
                    log.debug("Invalid argument format: {}", pair);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse arguments: {}", argsStr, e);
        }
        return args;
    }

    private List<Logs> filterLogsByIinBin(List<Logs> logs, String iinBinFilter) {
        if (iinBinFilter == null || iinBinFilter.trim().isEmpty()) {
            return logs;
        }

        return logs.stream()
                .filter(log -> {
                    Map<String, Object> args = log.getRequestArgs();
                    if (args == null) {
                        return false;
                    }
                    String iinValue = (String) args.get("iin_bin");
                    if (iinValue == null) {
                        iinValue = (String) args.get("iin");
                    }
                    return iinValue != null && iinValue.equals(iinBinFilter);
                })
                .collect(Collectors.toList());
    }

    private LocalDate getDateFromFileName(String fileName) {
        if ("active.log".equals(fileName)) {
            return LocalDate.now();
        }

        if (fileName.matches("\\d{4}-\\d{2}-\\d{2}\\.log")) {
            try {
                String dateStr = fileName.substring(0, fileName.lastIndexOf("."));
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                log.warn("Could not parse date from filename: {}", fileName, e);
            }
        }

        return null;
    }
}