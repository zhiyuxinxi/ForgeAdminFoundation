package com.mdframe.forge.starter.outbound.security;

import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistSaveRequest;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.service.impl.OutboundWhitelistServiceImpl;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiHttpToolPolicyTest {
    private final SysOutboundWhitelistMapper mapper = mock(SysOutboundWhitelistMapper.class);
    private final OutboundDnsResolver dns = mock(OutboundDnsResolver.class);
    private final DefaultOutboundPolicyService policy = new DefaultOutboundPolicyService(mapper, dns,
            new IpAddressClassifier(Set.of()), new OutboundHostNormalizer());

    @Test
    void absentAiRuleMustRejectBeforeDnsAndMustNotBorrowAnotherScene() {
        assertThrows(OutboundSecurityException.class, () -> policy.validate(context()));
        verify(mapper).selectActiveRules(1L, OutboundScenes.AI_HTTP_TOOL, "https", "allowed.example", 443);
        verifyNoInteractions(dns);
    }

    @Test
    void ownPublicRuleMustAllowButPrivateResolutionMustFailEvenWithLegacyFlag() throws Exception {
        SysOutboundWhitelist rule = new SysOutboundWhitelist();
        rule.setAllowPrivate(1);
        when(mapper.selectActiveRules(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(rule));
        when(dns.resolveAll("allowed.example")).thenReturn(List.of(InetAddress.getByAddress(new byte[]{8, 8, 8, 8})),
                List.of(InetAddress.getByAddress(new byte[]{10, 1, 2, 3})));
        assertEquals(OutboundScenes.AI_HTTP_TOOL, policy.validate(context()).getScene());
        assertThrows(OutboundSecurityException.class, () -> policy.validate(context()));
    }

    @Test
    void managementMustAcceptSameSceneButRejectPrivateException() {
        var service = new OutboundWhitelistServiceImpl(mapper, new OutboundHostNormalizer());
        OutboundWhitelistSaveRequest request = new OutboundWhitelistSaveRequest();
        request.setScene(OutboundScenes.AI_HTTP_TOOL);
        request.setProtocol("https");
        request.setHost("allowed.example");
        request.setPortStart(443);
        request.setPortEnd(443);
        request.setAllowPrivate(0);
        request.setStatus(1);
        when(mapper.insert(any(SysOutboundWhitelist.class))).thenReturn(1);
        assertEquals(OutboundScenes.AI_HTTP_TOOL, service.create(request).getScene());
        request.setAllowPrivate(1);
        assertThrows(BusinessException.class, () -> service.create(request));
        verify(mapper, times(1)).insert(any(SysOutboundWhitelist.class));
    }

    private OutboundRequestContext context() {
        return new OutboundRequestContext(OutboundScenes.AI_HTTP_TOOL, "https://allowed.example/data");
    }
}
