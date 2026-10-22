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
				order by conname
				""", String.class)).containsExactly("fk_opp_organizer", "fk_opp_type");
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
		assertThat(onDeleteRule("fk_opp_organizer")).isEqualTo("a");
		assertThat(onDeleteRule("fk_oaf_opportunity")).isEqualTo("c");
		assertThat(onDeleteRule("fk_oaf_art_field")).isEqualTo("r");

		var opportunityTypeId = jdbc.queryForObject(
				"select id from opportunity_types limit 1", UUID.class);
		assertThat(opportunityTypeId).isNotNull();
		var organizerId = jdbc.queryForObject(
				"select id from users where email = 'organizator@artconnect.test'", UUID.class);
		assertThat(organizerId).isNotNull();
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, street_address, city, latitude, longitude)
				values (?, ?, ?, 'Physical test', 'Description', now(),
				        'PHYSICAL', 'Test street', 'Zagreb', null, 15.98)
				""", UUID.randomUUID(), organizerId, opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline, location_type)
				values (?, ?, ?, 'Online test', 'Description', now(), 'ONLINE')
				""", UUID.randomUUID(), organizerId, opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url, event_start, event_end)
				values (?, ?, ?, 'Event test', 'Description', now(),
				        'ONLINE', 'https://example.test', now(), now() - interval '1 hour')
				""", UUID.randomUUID(), organizerId, opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url, status)
				values (?, ?, ?, 'Status test', 'Description', now(),
				        'ONLINE', 'https://example.test', 'UNKNOWN')
				""", UUID.randomUUID(), organizerId, opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline, location_type)
				values (?, ?, ?, 'Location test', 'Description', now(), 'HYBRID')
				""", UUID.randomUUID(), organizerId, opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url)
				values (?, ?, ?, 'Organizer test', 'Description', now(),
				        'ONLINE', 'https://example.test')
				""", UUID.randomUUID(), UUID.randomUUID(), opportunityTypeId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

		var retainedOpportunityId = UUID.randomUUID();
		jdbc.update("""
				insert into opportunities
					(id, organizer_id, type_id, title, description, deadline,
					 location_type, online_url)
				values (?, ?, ?, 'Organizer retention test', 'Description', now(),
				        'ONLINE', 'https://example.test')
				""", retainedOpportunityId, organizerId, opportunityTypeId);
		assertThatThrownBy(() -> jdbc.update("delete from users where id = ?", organizerId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThat(jdbc.queryForObject(
				"select count(*) from opportunities where id = ?", Integer.class, retainedOpportunityId))
				.isEqualTo(1);

		var liquibase = liquibase();
		int opportunityChangeSets = jdbc.queryForObject("""
				select count(*)
				from databasechangelog
				where orderexecuted >= (
				  select min(orderexecuted)
				  from databasechangelog
				  where id like 'OPP-01-DB-%'
				)
				""", Integer.class);
		assertThat(opportunityChangeSets).isGreaterThanOrEqualTo(36);
		try {
			liquibase.rollback(opportunityChangeSets, new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('opportunities') is null", Boolean.class))
				.isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('opportunity_art_fields') is null", Boolean.class))
				.isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('artwork_images') is null", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('artworks') is null", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('stored_files') is null", Boolean.class)).isTrue();

		liquibase = liquibase();
		try {
			liquibase.update(new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('opportunities') is not null", Boolean.class))
				.isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('artwork_images') is not null", Boolean.class))
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
	void artworkAndFileSchemaConstraintsIndexesCascadeAndRollback() throws Exception {
		assertThat(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conrelid in ('stored_files'::regclass, 'artworks'::regclass)
				  and contype = 'c'
				""", String.class)).containsExactlyInAnyOrder(
				"ck_stored_files_purpose", "ck_stored_files_content_type",
				"ck_stored_files_size_bytes", "ck_artworks_year", "ck_artworks_visibility",
				"ck_artworks_description_length");
		assertThat(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conrelid in ('stored_files'::regclass, 'artwork_images'::regclass)
				  and contype = 'u'
				""", String.class)).containsExactlyInAnyOrder(
				"uq_stored_files_storage_key", "uq_artwork_images_file_id");
		assertThat(jdbc.queryForList("""
				select conname
				from pg_constraint
				where conname in (
				  'fk_stored_files_owner', 'fk_artworks_artist',
				  'fk_artwork_images_artwork', 'fk_artwork_images_file'
				)
				""", String.class)).containsExactlyInAnyOrder(
				"fk_stored_files_owner", "fk_artworks_artist",
				"fk_artwork_images_artwork", "fk_artwork_images_file");

		assertThat(jdbc.queryForList("""
				select indexname
				from pg_indexes
				where schemaname = current_schema()
				  and tablename in ('stored_files', 'artworks', 'artwork_images')
				  and indexname like 'ix_%'
				""", String.class)).containsExactlyInAnyOrder(
				"ix_stored_files_owner", "ix_artworks_artist_visibility",
				"ix_artworks_artist_created", "ix_artwork_images_artwork");
		assertThat(jdbc.queryForObject("""
				select indexdef
				from pg_indexes
				where indexname = 'ix_artworks_artist_visibility'
				""", String.class)).contains("WHERE (hidden_by_admin = false)");
		assertThat(onDeleteRule("fk_stored_files_owner")).isEqualTo("c");
		assertThat(onDeleteRule("fk_artworks_artist")).isEqualTo("c");
		assertThat(onDeleteRule("fk_artwork_images_artwork")).isEqualTo("c");
		assertThat(onDeleteRule("fk_artwork_images_file")).isEqualTo("c");

		assertThat(jdbc.queryForObject("""
				select count(*) from artworks
				where artist_id = (select id from users where email = 'umjetnik@artconnect.test')
				""", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForList("""
				select visibility from artworks
				where artist_id = (select id from users where email = 'umjetnik@artconnect.test')
				order by visibility
				""", String.class)).containsExactly("PRIVATE", "PUBLIC");

		var ownerId = UUID.randomUUID();
		var artworkId = UUID.randomUUID();
		var fileId = UUID.randomUUID();
		jdbc.update("""
				insert into users (id, display_name)
				values (?, 'Cascade test user')
				""", ownerId);
		jdbc.update("""
				insert into artworks (id, artist_id, title, year, technique)
				values (?, ?, 'Cascade test artwork', 2024, 'Test')
				""", artworkId, ownerId);
		jdbc.update("""
				insert into stored_files
					(id, owner_id, purpose, original_name, content_type, size_bytes, storage_key)
				values (?, ?, 'ARTWORK_IMAGE', 'test.png', 'image/png', 1, ?)
				""", fileId, ownerId, UUID.randomUUID() + ".png");
		jdbc.update("""
				insert into artwork_images (id, artwork_id, file_id)
				values (?, ?, ?)
				""", UUID.randomUUID(), artworkId, fileId);
		assertThat(jdbc.queryForObject(
				"select visibility from artworks where id = ?", String.class, artworkId))
				.isEqualTo("PRIVATE");
		assertThat(jdbc.queryForObject(
				"select position from artwork_images where artwork_id = ?", Short.class, artworkId))
				.isZero();
		jdbc.update("delete from users where id = ?", ownerId);
		assertThat(jdbc.queryForObject(
				"select count(*) from artworks where id = ?", Integer.class, artworkId)).isZero();
		assertThat(jdbc.queryForObject(
				"select count(*) from artwork_images where artwork_id = ?", Integer.class, artworkId)).isZero();
		assertThat(jdbc.queryForObject(
				"select count(*) from stored_files where id = ?", Integer.class, fileId)).isZero();

		assertThatThrownBy(() -> jdbc.update("""
				insert into artworks (id, artist_id, title, year, technique, description)
				values (?, (select id from users where email = 'umjetnik@artconnect.test'),
				        'Too long description', 2024, 'Test', ?)
				""", UUID.randomUUID(), "x".repeat(5001)))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

		var liquibase = liquibase();
		int artworkChangeSets = jdbc.queryForObject("""
				select count(*) from databasechangelog where id like 'ART-01-DB-%'
				""", Integer.class);
		assertThat(artworkChangeSets).isEqualTo(18);
		try {
			liquibase.rollback(artworkChangeSets, new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('artwork_images') is null", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('artworks') is null", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select to_regclass('stored_files') is null", Boolean.class)).isTrue();

		liquibase = liquibase();
		try {
			liquibase.update(new Contexts("dev"), new LabelExpression());
		} finally {
			liquibase.close();
		}
		assertThat(jdbc.queryForObject("select to_regclass('artwork_images') is not null", Boolean.class))
				.isTrue();
	}

	@Test
	void contextLoads() {
	}
}
