package org.info.infobaza.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String WORD_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    // =============================================
    // 🔹 Обработка ошибок валидации
    // =============================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("⚠ IllegalArgumentException: {} (path={})", ex.getMessage(), getPath(request));

        if (isFileExportRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return buildJsonError(HttpStatus.BAD_REQUEST, "Bad Request", ex, request);
    }

    // =============================================
    // 🔹 Обработка NotFoundException
    // =============================================
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.warn("❌ NotFoundException: {} (path={})", ex.getMessage(), getPath(request));

        if (isFileExportRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return buildJsonError(HttpStatus.NOT_FOUND, "Not Found", ex, request);
    }

    // =============================================
    // 🔹 Глобальная обработка любых исключений
    // =============================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        log.error("💥 Exception at {}: {}", getPath(request), ex.getMessage(), ex);

        if (isFileExportRequest(request)) {
            // для PDF, Excel, Word — не возвращаем JSON
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return buildJsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex, request);
    }

    // =============================================
    // 🔧 Вспомогательные методы
    // =============================================
    private boolean isFileExportRequest(WebRequest request) {
        String contentType = request.getHeader("Content-Type");
        String accept = request.getHeader("Accept");

        return containsFileMime(contentType) || containsFileMime(accept);
    }

    private boolean containsFileMime(String header) {
        if (header == null) return false;
        return header.contains(EXCEL_CONTENT_TYPE)
                || header.contains(PDF_CONTENT_TYPE)
                || header.contains(WORD_CONTENT_TYPE);
    }

    private ResponseEntity<Map<String, Object>> buildJsonError(HttpStatus status, String error, Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", ex.getMessage());
        body.put("path", getPath(request));
        body.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(body, status);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
