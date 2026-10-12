package com.mdframe.forge.starter.outbound.client;

import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;

public interface SecureOutboundClient {

    OutboundResponse execute(OutboundRequest request);

    /** Register cancellation for the current call; null unregisters it after the call ends. */
    OutboundResponse execute(OutboundRequest request, java.util.function.Consumer<Runnable> cancellation);
}
