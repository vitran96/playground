package tech.kingoyster.spring_1.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false", // Don't run migration
        "spring.jpa.hibernate.ddl-auto=none", // Don't validate JPA
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
})
@AutoConfigureMockMvc(addFilters = false)
public class FileControllerIT {

    @Autowired
    private MockMvc mockMvc;
}
