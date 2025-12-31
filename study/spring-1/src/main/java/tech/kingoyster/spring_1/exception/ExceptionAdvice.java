package tech.kingoyster.spring_1.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionAdvice {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(Spring1Exception.class)
    public ResponseEntity<ErrorDetail> handleAppException(Spring1Exception e, WebRequest request) {
        HttpStatus httpStatus = extractStatusAnnotation(e);
        var error = new ErrorDetail(
                LocalDateTime.now(),
                -1,
                e.getMessage(),
                request.getContextPath(),
                null
        );

        return new ResponseEntity<>(error, httpStatus);
    }

    private static HttpStatus extractStatusAnnotation(Spring1Exception e) {
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleAppException(Exception e, WebRequest request) {
        var error = new ErrorDetail(
                LocalDateTime.now(),
                -1,
                e.getMessage(),
                request.getContextPath(),
                null
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
