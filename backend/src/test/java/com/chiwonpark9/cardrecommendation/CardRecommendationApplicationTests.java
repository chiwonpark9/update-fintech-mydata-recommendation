package com.chiwonpark9.cardrecommendation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class CardRecommendationApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
			.withDatabaseName("mydata_card_test")
			.withUsername("test_user")
			.withPassword("test_password");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoadsAndAppliesFlywayMigration() {
		String migrationMarker = jdbcTemplate.queryForObject(
				"SELECT metadata_value FROM service_metadata WHERE metadata_key = ?",
				String.class,
				"schema_initialized_by"
		);

		assertThat(migrationMarker).isEqualTo("flyway");
	}

}
