package com.worker.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConsumersMain {
    private static final Logger logger = LoggerFactory.getLogger(ConsumersMain.class);

    public static void main(final String[] args) throws Exception {
    	String[] ctxs ={"classpath*:/META-INF/spring/*-ctx.xml"};
    	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(ctxs, false, null);
        context.registerShutdownHook();
        try {
            context.refresh();
        } catch (Exception e) {
            logger.error("Problem in creating spring context", e);
            context.destroy();
        }
    }

}
