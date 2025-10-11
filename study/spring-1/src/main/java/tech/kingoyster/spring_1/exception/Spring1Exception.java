package tech.kingoyster.spring_1.exception;

public class Spring1Exception extends RuntimeException {
    public Spring1Exception(Throwable cause) {
        super(cause);
    }

    public Spring1Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public Spring1Exception(String message) {
        super(message);
    }
}
