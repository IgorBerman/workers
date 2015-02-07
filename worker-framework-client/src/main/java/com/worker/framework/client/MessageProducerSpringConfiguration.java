package com.worker.framework.client;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;


@Configuration
public class MessageProducerSpringConfiguration {

    @Inject private ProducerProperties properties;

    @Bean
    public ConnectionFactory connectionFactory() {
        com.rabbitmq.client.ConnectionFactory lowLevelFactory = new com.rabbitmq.client.ConnectionFactory();
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Message producer thread %d").build();
        lowLevelFactory.setThreadFactory(threadFactory);
        
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(lowLevelFactory);
        connectionFactory.setHost(properties.getQueueHostName());
        connectionFactory.setUsername(properties.getQueueUsername());
        connectionFactory.setPassword(properties.getQueuePassword());
        connectionFactory.setExecutor(daemonThreadsExecutor());
        return connectionFactory;
    }

    private Executor daemonThreadsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(1);
        executor.setCorePoolSize(1);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("Message producer executor thread");
        executor.initialize();
        return executor;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public AmqpTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setRetryTemplate(retryTemplate());
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetryInitialInterval());
        backOffPolicy.setMultiplier(properties.getRetryMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetryMaxInterval());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter() {
        JsonMessageConverter converter = new JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    @Bean
    public Queue workerQueue() {
        return new Queue(properties.getQueueName());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
