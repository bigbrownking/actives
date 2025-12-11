package org.info.infobaza.config.db;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "org.info.infobaza.repository.dossierprime",
        entityManagerFactoryRef = "dossierprimeEntityManagerFactory",
        transactionManagerRef = "dossierprimeTransactionManager")

public class DossierPrimeConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.dossierprime")
    public DataSourceProperties dossierprimeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.dossierprime.configuration")
    public DataSource dossierprimeDataSource() {
        return dossierprimeDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "dossierprimeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean dossierprimeEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        log.info("Connecting to dossierprime...");
        return builder
                .dataSource(dossierprimeDataSource())
                .packages("org.info.infobaza.model.dossierprime")
                .persistenceUnit("dossierprime")
                .properties(
                        Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                                "hibernate.hbm2ddl.auto", "none"))
                .build();
    }

    @Bean(name = "dossierprimeTransactionManager")
    public PlatformTransactionManager dossierprimeTransactionManager(
            @Qualifier("dossierprimeEntityManagerFactory") LocalContainerEntityManagerFactoryBean dossierEntityManagerFactory) {
        return new JpaTransactionManager(dossierEntityManagerFactory.getObject());
    }
}
