package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Assignment;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Owner;
import com.mdframe.forge.task.api.PeriodicTaskEngine.OwnerType;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;

/** Bounded direct membership only, never implicit descendants or name matching. */
@Component
@RequiredArgsConstructor
public class TaskPeriodAllocation {
    private final TenantMemberDirectory members;

    List<Assignment> resolve(Owner owner) {
        List<TenantMember> selected = switch (owner.type()) {
            case PERSON -> members.findActiveMembers(List.of(owner.id()), null);
            case DEPARTMENT -> members.findActiveMembersByOrg(owner.id(), null, null, 200);
            case POST -> members.findActiveMembersByPost(owner.id(), null, 200);
        };
        if (selected.isEmpty() || selected.size() > 200 || hasMore(owner, selected)) {
            throw TaskMessages.error(409, "period.assignment.invalid");
        }
        return selected.stream().map(member -> new Assignment(member.userId(), member.displayName())).toList();
    }

    private boolean hasMore(Owner owner, List<TenantMember> selected) {
        if (owner.type() == OwnerType.PERSON || selected.size() < 200) {
            return false;
        }
        long after = selected.get(selected.size() - 1).userId();
        return owner.type() == OwnerType.POST
                ? !members.findActiveMembersByPost(owner.id(), after, 1).isEmpty()
                : !members.findActiveMembersByOrg(owner.id(), null, after, 1).isEmpty();
    }
}
