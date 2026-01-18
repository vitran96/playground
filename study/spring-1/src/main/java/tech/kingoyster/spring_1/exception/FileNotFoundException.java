package tech.kingoyster.spring_1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileNotFoundException extends Spring1Exception {
    public FileNotFoundException(String filename) {
        super(filename + " not exists!");
    }

    public FileNotFoundException(String filename, Throwable cause) {
        super(filename + " not exists!", cause);
    }
}
