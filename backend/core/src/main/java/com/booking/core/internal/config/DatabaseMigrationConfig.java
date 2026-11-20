package com.booking.core.internal.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            DataSource ds = flyway.getConfiguration().getDataSource();

            // 1. Migrate core schema
            Flyway.configure()
                    .dataSource(ds)
                    .schemas("core")
                    .locations("classpath:db/migration/core")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            // 2. Migrate business schema
            Flyway.configure()
                    .dataSource(ds)
                    .schemas("business")
                    .locations("classpath:db/migration/business")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            // 3. Migrate customer schema
            Flyway.configure()
                    .dataSource(ds)
                    .schemas("customer")
                    .locations("classpath:db/migration/customer")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
        };
    }
}
