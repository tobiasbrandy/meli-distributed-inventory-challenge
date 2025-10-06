package com.tobiasbrandy.meli.inventory.repository;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.tobiasbrandy.meli.inventory.model")
@EnableJpaRepositories(basePackages = "com.tobiasbrandy.meli.inventory.repository")
class RepositoryTestApplication {
}
