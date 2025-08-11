package org.info.infobaza.config.db;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "org.info.infobaza.repository.ser",
        entityManagerFactoryRef = "serEntityManagerFactory",
        transactionManagerRef = "serTransactionManager"
)
public class SerDBConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.ser")
    public DataSourceProperties serDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.ser.configuration")
    public DataSource serDataSource() {
        return serDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "serEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean serEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(serDataSource())
                .packages("org.info.infobaza.model.ser")
                .persistenceUnit("ser")
                .properties(Map.of(
                        "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                        "hibernate.hbm2ddl.auto", "none"
                ))
                .build();
    }

    @Bean(name = "serTransactionManager")
    public PlatformTransactionManager serTransactionManager(
            @Qualifier("serEntityManagerFactory") LocalContainerEntityManagerFactoryBean serEntityManagerFactory) {
        return new JpaTransactionManager(serEntityManagerFactory.getObject());
    }
}