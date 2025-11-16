package tech.kingoyster.spring_1;

import lombok.Getter;

public enum HttpMediaType {
    APP_JSON("application/json"),
    ;

    @Getter
    private final String value;

    HttpMediaType(String contentType) {
        this.value = contentType;
    }
}
