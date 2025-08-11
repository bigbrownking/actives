package org.info.infobaza.config.db;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class InfoConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.info")
    public DataSourceProperties infoDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "infoDataSource")
    public DataSource infoDataSource() {
        return infoDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "infoJdbcTemplate")
    public JdbcTemplate infoJdbcTemplate(@Qualifier("infoDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "infoTransactionManager")
    public PlatformTransactionManager infoTransactionManager(@Qualifier("infoDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}