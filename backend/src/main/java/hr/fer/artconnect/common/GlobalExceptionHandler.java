package hr.fer.artconnect.common;

import jakarta.validation.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.context.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.security.access.*;
import org.springframework.security.core.context.*;
import org.springframework.validation.*;
import org.springframework.validation.method.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Locale HR = Locale.forLanguageTag("hr");
    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> apiError(ApiException exception) {
        if (exception.getCode() == ErrorCode.INTERNAL_ERROR) {
            return unexpectedError(exception);
        }
        return ResponseEntity.status(exception.getCode().getHttpStatus()).body(
                new ErrorResponse(exception.getCode(), exception.getMessage(), exception.getDetails()));
    }

    // MethodArgumentNotValidException nasljeđuje BindException.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> invalidFields(BindException exception) {
        List<FieldIssue> details = new ArrayList<>();
        for (var error : exception.getAllErrors()) {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : "body";
            String issue = error instanceof FieldError fieldError && fieldError.isBindingFailure()
                    ? message("validation.type.invalid") : messages.getMessage(error, HR);
            details.add(new FieldIssue(field, issue));
        }
        return validationError(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> invalidConstraints(ConstraintViolationException exception) {
        List<FieldIssue> details = new ArrayList<>();
        for (var error : exception.getConstraintViolations()) {
            details.add(new FieldIssue(error.getPropertyPath().toString(), error.getMessage()));
        }
        return validationError(details);
    }

    // Spring može ovako prijaviti neispravan parametar izravno na metodi kontrolera.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> invalidMethod(HandlerMethodValidationException exception) {
        if (exception.isForReturnValue()) {
            return unexpectedError(exception);
        }
        List<FieldIssue> details = new ArrayList<>();
        for (var result : exception.getParameterValidationResults()) {
            for (var error : result.getResolvableErrors()) {
                String field = result.getMethodParameter().getParameterName();
                if (result instanceof ParameterErrors && error instanceof FieldError fieldError) {
                    field = fieldError.getField();
                }
                details.add(new FieldIssue(field == null ? "body" : field, messages.getMessage(error, HR)));
            }
        }
        return validationError(details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> invalidInput(Exception exception) {
        String field = "body";
        String issue = "validation.body.invalid";
        if (exception instanceof MethodArgumentTypeMismatchException error) {
            field = error.getName();
            issue = "validation.type.invalid";
        } else if (exception instanceof MissingServletRequestParameterException error) {
            field = error.getParameterName();
            issue = "validation.required";
        } else if (exception instanceof MissingServletRequestPartException error) {
            field = error.getRequestPartName();
            issue = "validation.required";
        }
        return validationError(List.of(new FieldIssue(field, message(issue))));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return error(ErrorCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException exception) {
        // @PreAuthorize može odbiti i anonimnog korisnika na inače javnoj ruti.
        return error(SecurityErrorHandler.accessDeniedCode(exception,
                SecurityContextHolder.getContext().getAuthentication()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpectedError(Exception exception) {
        // Springove HTTP greške zadržavaju svoj status i zaglavlja (npr. Allow za 405).
        if (exception instanceof org.springframework.web.ErrorResponse response) {
            var status = response.getStatusCode();
            ErrorCode code = switch (status.value()) {
                case 401 -> ErrorCode.UNAUTHENTICATED;
                case 403 -> ErrorCode.FORBIDDEN;
                case 404 -> ErrorCode.NOT_FOUND;
                case 413 -> ErrorCode.FILE_TOO_LARGE;
                case 415 -> ErrorCode.UNSUPPORTED_FILE_TYPE;
                case 429 -> ErrorCode.RATE_LIMITED;
                default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.VALIDATION_ERROR;
            };
            if (status.is5xxServerError()) {
                log.error("Greška pri obradi zahtjeva", exception);
            }
            String key = status.value() == 405 ? "error.METHOD_NOT_ALLOWED" : "error." + code.name();
            return ResponseEntity.status(status).headers(response.getHeaders()).body(
                    new ErrorResponse(code, message(key), List.of()));
        }
        log.error("Greška pri obradi zahtjeva", exception);
        return error(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> validationError(List<FieldIssue> details) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.VALIDATION_ERROR, message("error.VALIDATION_ERROR"), details));
    }

    private ResponseEntity<ErrorResponse> error(ErrorCode code) {
        return ResponseEntity.status(code.getHttpStatus()).body(
                new ErrorResponse(code, message("error." + code.name()), List.of()));
    }

    private String message(String key) {
        return messages.getMessage(key, null, HR);
    }
}
