package tech.kingoyster.spring_1.dummy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class HelloResponse {
    private String data;
}
