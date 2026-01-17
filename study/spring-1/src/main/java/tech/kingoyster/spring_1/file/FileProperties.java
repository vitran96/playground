package tech.kingoyster.spring_1.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "app.file")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FileProperties {
    private Path directory = Path.of("").toAbsolutePath();
}
