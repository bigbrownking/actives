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
        basePackages = "org.info.infobaza.repository.dossier",
        entityManagerFactoryRef = "dossierEntityManagerFactory",
        transactionManagerRef = "dossierTransactionManager")
public class DossierDBConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.dossier")
    public DataSourceProperties dossierDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.dossier.configuration")
    public DataSource dossierDataSource() {
        return dossierDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "dossierEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean dossierEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dossierDataSource())
                .packages("org.info.infobaza.model.dossier")
                .persistenceUnit("dossier")
                .properties(
                        Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                                "hibernate.hbm2ddl.auto", "none"))
                .build();
    }

    @Bean(name = "dossierTransactionManager")
    public PlatformTransactionManager dossierTransactionManager(
            @Qualifier("dossierEntityManagerFactory") LocalContainerEntityManagerFactoryBean dossierEntityManagerFactory) {
        return new JpaTransactionManager(dossierEntityManagerFactory.getObject());
    }
}

