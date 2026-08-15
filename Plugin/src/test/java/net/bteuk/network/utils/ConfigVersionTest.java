package net.bteuk.network.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigVersionTest {

    @Test
    void testParse() {
        assertEquals(new ConfigVersion(1, 11, 0), ConfigVersion.parse("1.11.0"));
        assertEquals(new ConfigVersion(1, 11, 0), ConfigVersion.parse("1.11.0-SNAPSHOT"));
        assertEquals(new ConfigVersion(2, 0, 0), ConfigVersion.parse("2.0"));
        assertEquals(new ConfigVersion(3, 0, 0), ConfigVersion.parse("3"));
        assertEquals(new ConfigVersion(0, 0, 0), ConfigVersion.parse(null));
        assertEquals(new ConfigVersion(0, 0, 0), ConfigVersion.parse(""));
        assertEquals(new ConfigVersion(1, 2, 3), ConfigVersion.parse("1.2.3.4"));
    }

    @Test
    void testCompareTo() {
        ConfigVersion v1 = new ConfigVersion(1, 11, 0);
        ConfigVersion v2 = new ConfigVersion(1, 12, 0);
        ConfigVersion v3 = new ConfigVersion(2, 0, 0);
        ConfigVersion v1_patch = new ConfigVersion(1, 11, 1);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);
        assertTrue(v2.compareTo(v3) < 0);
        assertTrue(v1.compareTo(v1_patch) < 0);
        assertEquals(0, v1.compareTo(new ConfigVersion(1, 11, 0)));
    }

    @Test
    void testMigrationCondition() {
        ConfigVersion migrationVersion = new ConfigVersion(1, 12, 0);

        // Scenario: previous version is 1.11.0, current is 1.12.0
        // Should execute (previous < migration and current >= migration)
        ConfigVersion prev = new ConfigVersion(1, 11, 0);
        ConfigVersion curr = new ConfigVersion(1, 12, 0);
        assertTrue(prev.compareTo(migrationVersion) < 0 && curr.compareTo(migrationVersion) >= 0);

        // Scenario: previous version is 1.12.0, current is 1.13.0
        // Should NOT execute (previous is NOT < migration)
        prev = new ConfigVersion(1, 12, 0);
        curr = new ConfigVersion(1, 13, 0);
        assertFalse(prev.compareTo(migrationVersion) < 0 && curr.compareTo(migrationVersion) >= 0);

        // Scenario: previous version is 1.10.0, current is 1.11.0
        // Should NOT execute (current is NOT >= migration)
        prev = new ConfigVersion(1, 10, 0);
        curr = new ConfigVersion(1, 11, 0);
        assertFalse(prev.compareTo(migrationVersion) < 0 && curr.compareTo(migrationVersion) >= 0);
    }
}
