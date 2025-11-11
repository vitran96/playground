package tech.kingoyster.spring_1.dummy;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dummies")
public class DummyController {
    @GetMapping("/say-hi")
    public HelloResponse hello() {
        return HelloResponse.builder()
                .data("hi")
                .build();
    }

    @GetMapping("/private-hi")
    public HelloResponse privateHi() {
        return HelloResponse.builder()
                .data("...hi")
                .build();
    }
}
