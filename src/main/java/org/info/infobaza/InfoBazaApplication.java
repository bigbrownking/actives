package org.info.infobaza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, JdbcRepositoriesAutoConfiguration.class})
@EnableScheduling
@EnableCaching
public class InfoBazaApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfoBazaApplication.class, args);
    }

}


