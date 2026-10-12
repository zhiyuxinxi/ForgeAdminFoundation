package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.system.entity.SysClient;
import org.springframework.beans.BeanUtils;

/** Sa-Token 1.38 has no per-login concurrent/share fields; this isolated issuer retains the existing keyspace. */
final class ClientTokenIssuer extends StpLogic {
    private ClientTokenIssuer(String loginType, SaTokenConfig configuration) {
        super(loginType);
        setConfig(configuration);
    }

    @Override
    public StpLogic setLoginType(String loginType) {
        // The base constructor calls this method: never publish a request's configuration in SaManager.
        this.loginType = loginType;
        return this;
    }

    static void login(Long userId, SysClient client, String device) {
        StpLogic shared = StpUtil.getStpLogic();
        SaTokenConfig snapshot = new SaTokenConfig();
        BeanUtils.copyProperties(shared.getConfigOrGlobal(), snapshot, "activityTimeout");
        ClientPolicy policy = ClientPolicy.from(client, snapshot);
        snapshot.setIsConcurrent(policy.concurrent()).setIsShare(policy.share());
        ClientTokenIssuer issuer = new ClientTokenIssuer(shared.getLoginType(), snapshot);
        issuer.login(userId, new SaLoginModel().setDevice(device)
                .setTimeout(policy.timeout()).setActiveTimeout(policy.activeTimeout()));
    }

    private record ClientPolicy(long timeout, long activeTimeout, boolean concurrent, boolean share) {
        private static ClientPolicy from(SysClient client, SaTokenConfig defaults) {
            return new ClientPolicy(
                    client.getTokenTimeout() == null ? defaults.getTimeout() : client.getTokenTimeout(),
                    client.getTokenActivityTimeout() == null ? defaults.getActiveTimeout() : client.getTokenActivityTimeout(),
                    client.getConcurrentLogin() == null ? defaults.getIsConcurrent() : client.getConcurrentLogin(),
                    client.getShareToken() == null ? defaults.getIsShare() : client.getShareToken());
        }
    }
}
