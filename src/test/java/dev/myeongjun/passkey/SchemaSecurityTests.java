package dev.myeongjun.passkey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaSecurityTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatedTheFiveDomainTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                """,
                String.class
        );

        assertThat(tables).contains(
                "ACCOUNTS",
                "PASSKEY_CREDENTIALS",
                "WEBAUTHN_CEREMONIES",
                "PRIVATE_ITEMS",
                "SECURITY_EVENTS"
        );
    }

    @Test
    void schemaContainsNoPasswordOrPrivateKeyColumn() {
        Integer forbiddenColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND (COLUMN_NAME LIKE '%PASSWORD%' OR COLUMN_NAME LIKE '%PRIVATE_KEY%')
                """,
                Integer.class
        );

        assertThat(forbiddenColumnCount).isZero();
    }
}
