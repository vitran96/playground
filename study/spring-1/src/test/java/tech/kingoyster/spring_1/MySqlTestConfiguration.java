package tech.kingoyster.spring_1;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestConfiguration {

//    private static final String MYSQL_IMAGE = "docker.io/mysql:9.4.0-oraclelinux9";
    private static final String MYSQL_IMAGE = "mysql:9.4.0-oraclelinux9";

    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
                .withConfigurationOverride("mysql-config");
    }
}
