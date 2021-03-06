package com.worker.framework.client;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.common.collect.Lists;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageArg;

public class ProducerMain {
	@Configuration
	@PropertySource(ignoreResourceNotFound = true, value = {
			"classpath:message-producer.default.properties",
			"file:/etc/workers/message-producer.properties" })
	@ComponentScan(basePackages = { "com.worker.framework.client" })
	static class ProducerMainConfiguration{
		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}
		
	}

	//this is only an example
	public static void main(final String[] args) throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ProducerMainConfiguration.class);
		MessageProducerService producerService = ctx
				.getBean(MessageProducerService.class);
		WorkMessageArg arg = new WorkMessageArg("im", 1);
		producerService.submit("tenantId", new WorkMessage("DependencyTreeProcessor", Lists.newArrayList(arg)));
		ctx.close();
	}
}
