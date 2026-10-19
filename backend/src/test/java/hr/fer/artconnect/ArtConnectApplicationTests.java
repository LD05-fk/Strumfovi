package hr.fer.artconnect;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import javax.sql.DataSource;
import liquibase.*;
import liquibase.database.*;
import liquibase.database.jvm.*;
import liquibase.resource.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Provjerava da se cijeli Spring kontekst diže uz pravu PostgreSQL bazu (Testcontainers, potreban Docker).
@SpringBootTest
@Testcontainers
class ArtConnectApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	DataSource dataSource;

	@Test
	void opportunitySchemaConstraintsIndexesAndRollback() throws Exception {
		var checkConstraints = new HashSet<>(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conrelid = 'opportunities'::regclass and contype = 'c'
				""", String.class));
		assertThat(checkConstraints).containsExactlyInAnyOrder(
				"ck_opp_location_type", "ck_opp_status", "ck_opp_physical",
				"ck_opp_online", "ck_opp_event_end");

		assertThat(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conrelid = 'opportunities'::regclass and contype = 'f'
				""", String.class)).containsExactly("fk_opp_type");
		assertThat(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conrelid = 'opportunity_art_fields'::regclass and contype = 'f'
				order by conname
				""", String.class)).containsExactly("fk_oaf_art_field", "fk_oaf_opportunity");

		var indexes = new HashSet<>(jdbc.queryForList("""
				select indexname
				from pg_indexes
				where schemaname = current_schema()
				  and tablename in ('opportunities', 'opportunity_art_fields')
				  and indexname ~ '^ix_'
				""", String.class));
		assertThat(indexes).containsExactlyInAnyOrder(
				"ix_opp_public", "ix_opp_organizer", "ix_opp_city_lower",
				"ix_opp_open_deadline", "ix_opp_fts", "ix_opp_published_at",
				"ix_oaf_art_field");

		assertThat(onDeleteRule("fk_opp_type")).isEqualTo("r");
		assertThat(onDeleteRule("fk_oaf_opportunity")).isEqualTo("c");
		assertThat(onDeleteRule("fk_oaf_art_field")).isEqualTo("r");

		var opportunityTypeId = jdbc.queryForObject(
				"select id from opportunity_types limit 1", UUID.class);
		assertThat(opportunityTypeId).isNotNull();
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, street_address, city)
				values (?, ?, ?, 'Physical test', 'Description', now(),
				        'PHYSICAL', 'Test street', 'Zagreb')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline, location_type)
				values (?, ?, ?, 'Online test', 'Description', now(), 'ONLINE')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url, event_start, event_end)
				values (?, ?, ?, 'Event test', 'Description', now(),
				        'ONLINE', 'https://example.test', now(), now() - interval '1 hour')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url, status)
				values (?, ?, ?, 'Status test', 'Description', now(),
				        'ONLINE', 'https://example.test', 'UNKNOWN')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline, location_type)
				values (?, ?, ?, 'Location test', 'Description', now(), 'HYBRID')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

		var liquibase = liquibase();
		int opportunityChangeSets = jdbc.queryForObject("""
				select count(*)
				from databasechangelog
				where id like 'OPP-01-DB-%'
				""", Integer.class);
		assertThat(opportunityChangeSets).isPositive();
		try {
			liquibase.rollback(opportunityChangeSets, new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('opportunities') is null", Boolean.class))
				.isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('opportunity_art_fields') is null", Boolean.class))
				.isTrue();

		liquibase = liquibase();
		try {
			liquibase.update(new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('opportunities') is not null", Boolean.class))
				.isTrue();
	}

	private String onDeleteRule(String constraintName) {
		return jdbc.queryForObject(
				"select confdeltype::text from pg_constraint where conname = ?",
				String.class, constraintName);
	}

	private Liquibase liquibase() throws Exception {
		var connection = dataSource.getConnection();
		var database = DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(connection));
		return new Liquibase("db/changelog/db.changelog-master.yaml",
				new ClassLoaderResourceAccessor(), database);
	}

	@Test
	void contextLoads() {
	}
}
