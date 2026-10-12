package com.mdframe.forge.flow.client.event;

import com.mdframe.forge.flow.client.mapper.FlowEventInboxMapper;
import com.mdframe.forge.flow.client.FlowClientProperties;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FlowEventConsumerProperties.class)
@ConditionalOnProperty(prefix = "forge.flow.client.events", name = "enabled", havingValue = "true")
public class FlowEventConsumerConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "forge.flow.client.events", name = "acknowledgement-enabled", havingValue = "true")
    public FlowEventAcknowledgementClient flowEventAcknowledgementClient(FlowEventConsumerProperties consumer,
            FlowClientProperties client, SecureOutboundClient outbound) {
        return new FlowEventAcknowledgementClient(consumer, client, outbound);
    }

    @Bean
    public FlowDurableEventConsumer flowDurableEventConsumer(FlowEventConsumerProperties properties,
            ApplicationContext context, FlowEventInboxMapper mapper, DataSource dataSource,
            PlatformTransactionManager transactionManager) {
        return new FlowDurableEventConsumer(properties, new FlowCallbackCatalog(context),
                mapper, dataSource, transactionManager);
    }
}
