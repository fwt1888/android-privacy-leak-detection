package edu.njupt.flowanalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogBack {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
//        org.apache.log4j.LogManager.resetConfiguration();
//        org.apache.log4j.PropertyConfigurator.configure("src/main/resources/log4j.properties");
        LogBack logback = new LogBack();
        logback.testLog();
    }

    private void testLog() {
        logger.debug("print debug log.");
        logger.info("print info log.");
        logger.error("print error log.");
    }
}