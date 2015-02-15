package com.worker.framework;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tenant.framework.CurrentTenant;
import com.tenant.framework.TenantDbParamsTemplateResolver;
import com.tenant.framework.TenantIdsResolver;
import com.worker.framework.api.WorkerProperties;
import com.worker.framework.internalapi.ControlQueueMessageListener;
import com.worker.framework.internalapi.Worker;
import com.worker.framework.internalapi.WorkerQueueMessageListener;
import com.worker.framework.messagehandlingchain.ExecuteWorker;
import com.worker.framework.messagehandlingchain.SubmitJoinMessage;
import com.worker.framework.messagehandlingchain.SubmitNewTasks;
import com.worker.framework.messagehandlingchain.VerifyChildrenInheritJoinState;
import com.worker.framework.messagehandlingchain.WMProcessor;
import com.worker.framework.messagehandlingchain.WatchdogProcessingTime;
import com.worker.framework.monitoring.ProcessorsFinishInTimeWatchDogAndTimer;
import com.worker.framework.python.PythonWorkerConf;
import com.worker.framework.python.PythonWorkerConnectionUrl;
import com.worker.framework.recovery.LogOnRetryListener;
import com.worker.framework.recovery.RepublishMessageRecovererForControlMessages;
import com.worker.framework.recovery.RepublishMessageRecovererWithJoinSupport;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.framework.tenant.MultiTenantMessageListenerAdapter;


@Configuration
public class WorkerFrameworkSpringConfiguration {    
	private static final String THREAD_SCOPE = "thread";//used also in ctx.xml
    private static final String DEFAULT_EXCHANGE = "";
    @Inject private WorkerProperties properties;
    @Inject private CurrentTenant currentTenant;
    @Inject private ControlQueueMessageListener controlListener;
    @Inject private TenantIdsResolver tenantIdsResolver;
    @Inject private TenantDbParamsTemplateResolver templateResolver;
    @Inject private List<Worker> workers;
    @Inject private ProcessorsFinishInTimeWatchDogAndTimer watchdog;

    @Bean
    public ConnectionFactory connectionFactory() {
        com.rabbitmq.client.ConnectionFactory lowLevelFactory = new com.rabbitmq.client.ConnectionFactory();
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Rabbitmq Connection thread %d").build();
        lowLevelFactory.setThreadFactory(threadFactory);
        
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(lowLevelFactory);
        connectionFactory.setHost(properties.getQueueHostName());
        connectionFactory.setUsername(properties.getQueueUsername());
        connectionFactory.setPassword(properties.getQueuePassword());
        int totalConsumers = properties.getConcurrentConsumers()+properties.getConcurrentLowConsumers()+1;
        connectionFactory.setChannelCacheSize(totalConsumers);
        return connectionFactory;
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer() {
        int concurrentConsumers = properties.getConcurrentConsumers();
        if (concurrentConsumers != 0) {
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setRabbitAdmin(amqpAdmin());
            container.setConnectionFactory(connectionFactory());
            container.setQueueNames(properties.getQueueName());
            container.setConcurrentConsumers(concurrentConsumers);
            container.setAcknowledgeMode(AcknowledgeMode.AUTO);
            container.setAdviceChain(workMessagesAdviceChain()); // for retry logic
            container.setMessageListener(multiTenantMessageListener());
            container.setTaskExecutor(executor(properties.getQueueName(), concurrentConsumers, false));
            container.setMissingQueuesFatal(false);
            //container.setPrefetchCount(concurrentConsumers*2);
            return container;
        }
        return null;
    }
    
    
    @Bean
    public SimpleMessageListenerContainer lowMessageListenerContainer() {
        int concurrentLowConsumers = properties.getConcurrentLowConsumers();
        if (concurrentLowConsumers != 0 ){
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setRabbitAdmin(amqpAdmin());
            container.setConnectionFactory(connectionFactory());
            container.setQueueNames(properties.getQueueName(), properties.getLowQueueName());
            container.setConcurrentConsumers(concurrentLowConsumers);
            container.setAcknowledgeMode(AcknowledgeMode.AUTO);
            container.setAdviceChain(workMessagesAdviceChain()); // for retry logic
            container.setMessageListener(multiTenantMessageListener());
            container.setTaskExecutor(executor(properties.getQueueName() + properties.getLowQueueName(), 
                                               concurrentLowConsumers, false));
            container.setMissingQueuesFatal(false);
            //container.setPrefetchCount(concurrentLowConsumers);
            return container;
        }
        return null;
    }
    
    @Bean
    public SimpleMessageListenerContainer controlMessageListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(properties.getControlQueueName());
        container.setConcurrentConsumers(1);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setMessageListener(controlMultiTenantMessageListener());
        //container.setPrefetchCount(5);
        container.setAdviceChain(controlMessagesAdviceChain()); // for retry logic
        container.setTaskExecutor(executor(properties.getControlQueueName(), 1, false));
        container.setMissingQueuesFatal(false);
        return container;
    }
    
    @Bean
    public WorkerQueueMessageListener listener() {
        WMProcessor firstInChain = new ExecuteWorker(workers);
        WMProcessor second = new VerifyChildrenInheritJoinState(firstInChain);
        WMProcessor third  = new SubmitNewTasks(properties, messagesTemplate(), second);
        WMProcessor forth  = new SubmitJoinMessage(controlMessagesTemplate(), third); 
        WMProcessor chain =  new WatchdogProcessingTime(watchdog, forth);
        return new WorkerQueueMessageListenerImpl(chain);
    }

    private Advice[] workMessagesAdviceChain() {
        return new Advice[] {workMessagesRetryInterceptor()};
    }
    private Advice[] controlMessagesAdviceChain() {
        return new Advice[] {controlMessagesRetryInterceptor()};
    }
    
    private ThreadPoolTaskExecutor executor(String threadPrefix, int size, boolean daemon) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(size);
        executor.setCorePoolSize(size);
        executor.setDaemon(daemon);//consumers will hold jvm up & running
        executor.setThreadNamePrefix(threadPrefix);
        executor.initialize();
        return executor;
    }

    private MultiTenantMessageListenerAdapter multiTenantMessageListener() {
        return new MultiTenantMessageListenerAdapter(listener(), jsonMessageConverter(), currentTenant,
                properties.getTenantIdHeader());
    }
    
    private MultiTenantMessageListenerAdapter controlMultiTenantMessageListener() {
        return new MultiTenantMessageListenerAdapter(controlListener, jsonMessageConverter(), currentTenant,
                properties.getTenantIdHeader());
    }

    @Bean
    public PythonWorkerConf pythonWorkerConf() {
        File file = new File(properties.getPidDir());
        file.mkdirs();

        List<PythonWorkerConnectionUrl> connectionUrls = Lists.newArrayList();
        for (String tenantId : tenantIdsResolver.getIds()) {
            connectionUrls.add(new PythonWorkerConnectionUrl(tenantId, properties.getDatabaseDialect(),
                    properties.getDatabaseHost(),
                    templateResolver.resolveDbName(properties.getDatabaseName(), tenantId),
                    templateResolver.resolveUser(properties.getDatabaseUsername(), tenantId),
                    templateResolver.resolvePassword(properties.getDatabasePassword(), tenantId)));
        }

        try {
			return new PythonWorkerConf(properties.getPythonCodeDir(), properties.getPidDir(), connectionUrls,
					properties.getPythonBinPath(), properties.getPythonSubprocessMain(), properties.getPythonLogPathPrefix());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to get path  of worker.py", e);
		}
    }

    @Bean
    public RetryOperationsInterceptor workMessagesRetryInterceptor() {
        return RetryInterceptorBuilder.stateless().retryOperations(retryTemplate(properties.getRetryMaxAttempts()))
                .recoverer(workerMessageRecoverer())
                .build();
    }
    
    
    @Bean
    public RetryOperationsInterceptor controlMessagesRetryInterceptor() {
        return RetryInterceptorBuilder.stateless().retryOperations(retryTemplate(properties.getControlRetryMaxAttempts()))                
                                      .recoverer(controlMessageRecoverer())
                                      .build();
    }

    @Bean
    public MessageRecoverer workerMessageRecoverer() {
        //we want publish low & normal messages into same regular error queue
        //we specify exchange name since otherwise error of low message will be published into error.low.worker.queue
        RepublishMessageRecovererWithJoinSupport recoverer 
            = new RepublishMessageRecovererWithJoinSupport(properties, controlMessagesTemplate(), DEFAULT_EXCHANGE, messagesTemplate(), jsonMessageConverter());
        return recoverer;
    }
    
    
    @Bean
    public MessageRecoverer controlMessageRecoverer() {
        //we want publish control messages into regular error queue
        //we specify exchange name since otherwise error of control message will be published into error.control.worker.queue
        return new RepublishMessageRecovererForControlMessages(properties, controlMessagesTemplate(), DEFAULT_EXCHANGE, messagesTemplate(), jsonMessageConverter());
    }

    @Bean
    @PostConstruct
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    @Primary
    public MultiTenantAmqpTemplate messagesTemplate() {
        MultiTenantAmqpTemplate rabbitTemplate =
                new MultiTenantAmqpTemplate(connectionFactory(), currentTenant, jsonMessageConverter(),
                        properties.getQueueName(), properties.getTenantIdHeader());
        rabbitTemplate.setRetryTemplate(retryTemplate(properties.getRetryMaxAttempts()));
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
    
    @Bean
    public MultiTenantAmqpTemplate controlMessagesTemplate() {
        MultiTenantAmqpTemplate rabbitTemplate =
                new MultiTenantAmqpTemplate(connectionFactory(), currentTenant, jsonMessageConverter(),
                        properties.getControlQueueName(), properties.getTenantIdHeader());
        rabbitTemplate.setRetryTemplate(retryTemplate(properties.getRetryMaxAttempts()));
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    private RetryTemplate retryTemplate(int maxRetries) {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetryInitialInterval());
        backOffPolicy.setMultiplier(properties.getRetryMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetryMaxInterval());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new LogOnRetryListener());
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
    public Queue lowWorkerQueue() {
        return new Queue(properties.getLowQueueName());
    }

    @Bean
    public Queue errorQueue() {
        return new Queue(properties.getErrorQueueName());
    }
    
    @Bean
    public Queue controlQueue() {
        return new Queue(properties.getControlQueueName());
    }

    @Bean
    public static CustomScopeConfigurer registerThreadLocalScope() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.setScopes(ImmutableMap.<String, Object> of(THREAD_SCOPE, SimpleThreadScope.class));
        return configurer;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
