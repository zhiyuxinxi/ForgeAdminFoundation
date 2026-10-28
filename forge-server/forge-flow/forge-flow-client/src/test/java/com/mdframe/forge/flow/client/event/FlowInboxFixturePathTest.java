package com.mdframe.forge.flow.client.event;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowInboxFixturePathTest {
    @Test
    void realMigrationAndMapperWorkFromRepositoryServerAndModuleDirectories() throws Exception {
        Path classes = Path.of(FlowInboxFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path module = classes.getParent().getParent();
        Path repository = module.resolve("../../..").normalize();
        for (Path directory : List.of(repository, repository.resolve("forge-server"), module)) {
            try (var fixture = new FlowInboxFixture(directory)) {
                fixture.add("approved");
                assertEquals("event-one", fixture.consumer().consume(FlowInboxFixture.event()).eventId());
                assertEquals(1, fixture.completed(), "real migration and Mapper must work from " + directory);
            }
        }
    }
}
