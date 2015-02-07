package com.worker.framework.monitoring;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.worker.framework.internalapi.WorkerProperties;


@Component
//open in browser http://localhost:8181/workersAdmin/
public class WorkerAdminServer {
    private static final String WORKERS_ADMIN = "/admin";
	private static final Logger logger = LoggerFactory.getLogger(WorkerAdminServer.class);
    @Inject MetricRegistry metricRegistry;
    @Inject HealthCheckRegistry healthCheckRegistry;
    @Inject WorkerProperties properties;
    private Server server;

    @PostConstruct
    public void init() throws Exception {
        QueuedThreadPool serverThreadPool = new QueuedThreadPool();
        serverThreadPool.setDaemon(true);
        serverThreadPool.setName("jetty");
        server = new Server(serverThreadPool);
        
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(properties.getAdminPort());
        server.setConnectors(new Connector[]{connector});
        
        ServletContextHandler adminContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        adminContext.setContextPath("/");
        server.setHandler(adminContext);
        
        adminContext.addServlet(AdminServlet.class, WORKERS_ADMIN +"/*");
        adminContext.addServlet(DefaultServlet.class, "/");
        adminContext.addEventListener(new WorkerMetricsServletListener());
        adminContext.addEventListener(new WorkerHealthServletListener());
        
        server.start();
    }

    @PreDestroy
    public void destroy() {
        try {
            if (server.isRunning()){
                server.stop();
                server.destroy();
            }
        } catch(Throwable e) {
            logger.error("Problem stopping admin server", e);
        }
    }
    
    private class WorkerMetricsServletListener extends MetricsServlet.ContextListener {
        @Override
        protected MetricRegistry getMetricRegistry() {
            return metricRegistry;
        }
        @Override
        public void contextInitialized(ServletContextEvent event) {
        	logger.info("Starting metrics registry");
        	super.contextInitialized(event);
        }
    }
    private class WorkerHealthServletListener extends HealthCheckServlet.ContextListener {
        @Override
        protected HealthCheckRegistry getHealthCheckRegistry() {
            return healthCheckRegistry;
        }
        @Override
        public void contextInitialized(ServletContextEvent event) {
        	logger.info("Starting health checks registry");
        	super.contextInitialized(event);
        }
    }
}
