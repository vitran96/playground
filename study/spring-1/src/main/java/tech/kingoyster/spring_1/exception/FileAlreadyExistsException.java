package tech.kingoyster.spring_1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileAlreadyExistsException extends Spring1Exception {
    public FileAlreadyExistsException(String filename) {
        super(filename + " already exists!");
    }

    public FileAlreadyExistsException(String filename, Throwable cause) {
        super(filename + " already exists!", cause);
    }
}
