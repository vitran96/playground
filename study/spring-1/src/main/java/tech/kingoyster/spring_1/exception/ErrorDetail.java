package tech.kingoyster.spring_1.exception;

import java.time.LocalDateTime;

public record ErrorDetail(LocalDateTime timestamp, int code, String message, String path, Object detail) {
}
