package com.eaglebank.common.error;

import com.eaglebank.model.BadRequestErrorResponse;
import com.eaglebank.model.ErrorResponse;
import com.eaglebank.model.FieldErrorDetail;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Translates validation and application failures into OpenAPI error responses.
 *
 * @author mattbateup
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_REQUEST = "Invalid details supplied";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException exception) {
        if (exception.status().is5xxServerError()) {
            log.error("Request failed", exception);
        }
        return ResponseEntity.status(exception.status()).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<BadRequestErrorResponse> handleBody(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> details = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.add(detail(error.getField(), error.getDefaultMessage(), error.getCode()));
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            details.add(detail(error.getObjectName(), error.getDefaultMessage(), error.getCode()));
        }
        return badRequest(details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<BadRequestErrorResponse> handleMethod(HandlerMethodValidationException exception) {
        List<FieldErrorDetail> details = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            String fallback = parameter == null ? "request" : parameter;
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error instanceof FieldError fieldError) {
                    details.add(detail(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getCode()));
                } else if (error instanceof ObjectError objectError) {
                    String field = objectError.getObjectName();
                    if (field == null || field.isBlank() || field.equals(result.getMethodParameter().getExecutable().getName())) {
                        field = fallback;
                    }
                    details.add(detail(field, objectError.getDefaultMessage(), objectError.getCode()));
                } else {
                    details.add(detail(fallback, error.getDefaultMessage(), typeOf(error)));
                }
            }
        });
        return badRequest(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<BadRequestErrorResponse> handleConstraint(ConstraintViolationException exception) {
        List<FieldErrorDetail> details = exception.getConstraintViolations().stream()
                .map(this::fromViolation)
                .toList();
        return badRequest(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<BadRequestErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String field = "body";
        String message = "Request body is invalid";
        String type = "invalid";
        if (exception.getMessage() != null && exception.getMessage().contains("Required request body is missing")) {
            message = "Request body is required";
        } else if (cause instanceof UnrecognizedPropertyException unrecognized) {
            field = unrecognized.getPropertyName();
            message = "Unknown property";
            type = "unknown";
        } else if (cause instanceof InvalidFormatException invalid) {
            field = path(invalid);
            message = "Invalid value";
        } else if (cause instanceof MismatchedInputException mismatched) {
            field = path(mismatched);
            message = "Invalid value";
        }
        return badRequest(List.of(detail(field, message, type)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
    }

    private FieldErrorDetail fromViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        int dot = field.lastIndexOf('.');
        if (dot >= 0) {
            field = field.substring(dot + 1);
        }
        String type = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        return detail(field, violation.getMessage(), type);
    }

    private static ResponseEntity<BadRequestErrorResponse> badRequest(List<FieldErrorDetail> details) {
        List<FieldErrorDetail> body = details.isEmpty()
                ? List.of(detail("request", INVALID_REQUEST, "invalid"))
                : details;
        return ResponseEntity.badRequest().body(new BadRequestErrorResponse(INVALID_REQUEST, body));
    }

    private static FieldErrorDetail detail(String field, String message, String type) {
        String safeField = field == null || field.isBlank() ? "request" : field;
        String safeMessage = message == null || message.isBlank() ? "is invalid" : message;
        String safeType = type == null || type.isBlank() ? "invalid" : type;
        return new FieldErrorDetail(safeField, safeMessage, safeType);
    }

    private static String typeOf(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        if (codes == null || codes.length == 0 || codes[codes.length - 1] == null) {
            return "invalid";
        }
        return codes[codes.length - 1];
    }

    private static String path(JsonMappingException exception) {
        String field = exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining("."));
        return field.isBlank() ? "body" : field;
    }
}