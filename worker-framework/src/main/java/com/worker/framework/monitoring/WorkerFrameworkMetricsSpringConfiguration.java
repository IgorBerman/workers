package com.worker.framework.monitoring;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.base.Strings;
import com.tenant.framework.CurrentTenant;
import com.worker.framework.api.WorkerProperties;


@Configuration
public class WorkerFrameworkMetricsSpringConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(WorkerFrameworkMetricsSpringConfiguration.class);
    @Inject private WorkerProperties properties;
    @Inject private CurrentTenant currentTenant;

    @Bean
    public MetricRegistry metricsRegistry() {

        MetricRegistry registry = new MetricRegistry();
        registry.register("jvm.gc", new GarbageCollectorMetricSet());
        registry.register("jvm.memory", new MemoryUsageGaugeSet());
        registry.register("jvm.thread-states", new ThreadStatesGaugeSet());
        registry.register("jvm.fd.usage", new FileDescriptorRatioGauge());
        return registry;
    }

    @Bean
    public ProcessorsFinishInTimeWatchDogAndTimer watchdogWithTimer() {
        return new ProcessorsFinishInTimeWatchDogAndTimer(properties.getProcessorsTimeThreshold(), properties.getLowPriorityProcessorsTimeThreshold(), metricsRegistry(),
                currentTenant);
    }

    @Bean
    public HealthCheckRegistry healthRegistry() {
        HealthCheckRegistry registry = new HealthCheckRegistry();
        registry.register("processorsFinishInTime", watchdogWithTimer());
        return registry;
    }

    @Bean(name = "console reporter", destroyMethod = "close")
    public ConsoleReporter configureConsoleReporter() {
        final ConsoleReporter reporter =
                ConsoleReporter.forRegistry(metricsRegistry()).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        reporter.start(5, TimeUnit.MINUTES);
        return reporter;
    }

    @Bean(name = "graphite reporter", destroyMethod = "close")
    public GraphiteReporter configureGraphiteReporter() {
        if (Strings.isNullOrEmpty(properties.getGraphiteServer()) || properties.getGraphitePort() == null) {
            return null;
        }
        InetSocketAddress addr =
                new InetSocketAddress(properties.getGraphiteServer(), properties.getGraphitePort().intValue());
        final Graphite graphite = new Graphite(addr);
        String hostName = System.getenv("HOSTNAME");
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("Unable to resolve hostname ", e);
        }
        logger.info("Configuring graphite reporter to " + addr.toString() + " from " + hostName);
        final GraphiteReporter reporter =
                GraphiteReporter.forRegistry(metricsRegistry()).prefixedWith(hostName).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
        reporter.start(1, TimeUnit.MINUTES);
        return reporter;
    }

}
