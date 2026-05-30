package com.sara.superadmin.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
		return body(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		FieldError fe = ex.getBindingResult().getFieldError();
		String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : "Validation failed";
		return body(HttpStatus.BAD_REQUEST, msg);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
		return body(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
	}

	private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
		Map<String, Object> b = new LinkedHashMap<>();
		b.put("timestamp", Instant.now().toString());
		b.put("status", status.value());
		b.put("error", status.getReasonPhrase());
		b.put("message", message);
		return ResponseEntity.status(status).body(b);
	}
}
