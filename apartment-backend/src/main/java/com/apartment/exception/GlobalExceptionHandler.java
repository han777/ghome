package com.apartment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBizEx(BusinessException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.getErrorCode().getCode());
        body.put("args", e.getArgs());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleSQLConstraint(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "general.DATA_CONSTRAINT_VIOLATION");
        body.put("args", new Object[0]);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeEx(RuntimeException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "runtime.UNEXPECTED");
        body.put("message", e.getMessage());
        body.put("args", new Object[0]);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralEx(Exception e) {
        e.printStackTrace();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "general.INTERNAL_ERROR");
        body.put("args", new Object[0]);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
