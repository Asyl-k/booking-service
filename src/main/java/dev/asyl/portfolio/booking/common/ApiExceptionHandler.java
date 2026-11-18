package dev.asyl.portfolio.booking.common;

import dev.asyl.portfolio.booking.reservation.BookingConflictException;
import dev.asyl.portfolio.booking.reservation.BookingNotFoundException;
import dev.asyl.portfolio.booking.reservation.InvalidBookingIntervalException;
import dev.asyl.portfolio.booking.reservation.StartsAtInPastException;
import dev.asyl.portfolio.booking.resource.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid",
                "validation_failed"
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                exception.getMessage(),
                "validation_failed"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request body is missing or contains invalid JSON",
                "malformed_request"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter",
                "Parameter '%s' has an invalid value".formatted(exception.getName()),
                "invalid_parameter"
        );
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSort(PropertyReferenceException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid sort parameter",
                exception.getMessage(),
                "invalid_sort"
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Missing parameter",
                "Parameter '%s' is required".formatted(exception.getParameterName()),
                "missing_parameter"
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), "resource_not_found");
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ProblemDetail handleBookingNotFound(BookingNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Booking not found", exception.getMessage(), "booking_not_found");
    }

    @ExceptionHandler(InvalidBookingIntervalException.class)
    public ProblemDetail handleInvalidInterval(InvalidBookingIntervalException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid interval", exception.getMessage(), "invalid_interval");
    }

    @ExceptionHandler(StartsAtInPastException.class)
    public ProblemDetail handleStartsAtInPast(StartsAtInPastException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid start time", exception.getMessage(), "starts_at_in_past");
    }

    @ExceptionHandler(BookingConflictException.class)
    public ProblemDetail handleBookingConflict(BookingConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Booking conflict", exception.getMessage(), "booking_conflict");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataConflict(DataIntegrityViolationException exception) {
        log.debug("Data integrity violation", exception);
        return problem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "The operation conflicts with existing data",
                "data_conflict"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", "The requested resource does not exist", "not_found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                exception.getMessage(),
                "method_not_allowed"
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred",
                "internal_error"
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
