package com.mdframe.forge.task.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Calendar rules are immutable versions; dates use the rule zone and persisted instants use UTC. */
public interface PeriodicTaskEngine {
    Schedule define(Define command);
    Schedule get(long id);
    List<Schedule> list(Long afterId, int limit);
    Schedule enable(long id, boolean enabled);
    List<Occurrence> preview(Calendar calendar, int fromIndex, int count);
    Run generate(long id, int limit);

    enum Cycle { ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM_DAYS }
    enum OwnerType { PERSON, DEPARTMENT, POST }

    record Calendar(Cycle cycle, int frequency, LocalDate firstDate, String zone,
                    int advanceDays, int windowDays, Integer endCount, LocalDate endDate) { }
    record Owner(OwnerType type, long id) { }
    record Define(String code, int version, long definitionId, String sourceKey,
                  String title, Calendar calendar, Owner owner, boolean allowLate) { }
    record Schedule(long id, Define rule, boolean enabled, int nextIndex) { }
    record Occurrence(int index, LocalDate startDate, LocalDateTime generateAtUtc,
                      LocalDateTime startsAtUtc, LocalDateTime dueAtUtc) { }
    record Assignment(long userId, String displayName) { }
    record Run(long scheduleId, int generatedPeriods, int nextIndex, boolean exhausted) { }
}
