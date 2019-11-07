/*
 * Copyright (c) 2019 - Felipe Desiderati ALL RIGHTS RESERVED.
 *
 * This software is protected by international copyright laws and cannot be
 * used, copied, stored or distributed without prior authorization.
 */
package br.tech.desiderati.common.configuration;

import br.tech.desiderati.common.tenant.MultiTenancyConnectionProvider;
import br.tech.desiderati.common.tenant.MultiTenancyDatabaseMigrationInitializer;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootConfiguration
@AutoConfigureBefore({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(MultiTenancyProperties.class)
@ConditionalOnProperty(prefix = "app.multitenancy", name = "type", havingValue = "schema")
public class MultiTenancySchemaConfiguration {

    private final MultiTenancyDatabaseMigrationInitializer multiTenancyDatabaseMigrationInitializer;

    @Autowired
    public MultiTenancySchemaConfiguration(MultiTenancyDatabaseMigrationInitializer multiTenancyDatabaseMigrationInitializer) {
        this.multiTenancyDatabaseMigrationInitializer = multiTenancyDatabaseMigrationInitializer;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(MultiTenancyConnectionProvider multiTenancyConnectionProvider,
                                                                       CurrentTenantIdentifierResolver tenantIdentifierResolver) {
        return hibernateProperties -> {
            hibernateProperties.put(Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
            hibernateProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenancyConnectionProvider);
            hibernateProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        };
    }
}