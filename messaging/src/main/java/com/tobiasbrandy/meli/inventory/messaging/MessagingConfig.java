package com.tobiasbrandy.meli.inventory.messaging;

import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Wires Redis Stream consumption with a single-consumer-per-group setup.
 * <p>
 * Creates consumer groups if absent and subscribes the {@link EventListener} to
 * the configured streams.
 */
@Configuration
class MessagingConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container(final RedisConnectionFactory cf) {
        return StreamMessageListenerContainer.create(cf, StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .builder()
            .pollTimeout(Duration.ofSeconds(1))
            .batchSize(10)
            .build());
    }

    @Bean
    List<Subscription> subscriptions(
        final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
        final StringRedisTemplate redis,
        final EventListener listener,
        @Qualifier("consumerGroup") final String consumerGroup,
        @Qualifier("consumerStreams") final List<String> consumerStreams
    ) {
        final List<Subscription> subscriptions = new ArrayList<>(consumerStreams.size());

        for (val stream : consumerStreams) {
            try {
                redis.opsForStream().createGroup(stream, ReadOffset.from("0"), consumerGroup);
            } catch (final Exception e) {
                // Group already exists
            }

            val sub = container.receiveAutoAck(
                Consumer.from(consumerGroup, consumerGroup), // We use the same since we only have 1 consumer per consumer group
                StreamOffset.create(stream, ReadOffset.lastConsumed()),
                listener
            );

            subscriptions.add(sub);
        }

        return subscriptions;
    }
}
