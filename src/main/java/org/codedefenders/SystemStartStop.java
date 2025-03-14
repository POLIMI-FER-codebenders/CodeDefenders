/*
 * Copyright (C) 2016-2020 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.configuration.ConfigurationValidationException;
import org.codedefenders.execution.ThreadPoolManager;
import org.codedefenders.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import io.prometheus.client.exporter.MetricsServlet;
import net.bull.javamelody.ReportServlet;

@WebListener
public class SystemStartStop implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(SystemStartStop.class);

    @Inject
    private ThreadPoolManager mgr;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Configuration config;

    @Inject
    private MetricsService metricsService;

    /**
     * This method is called when the servlet context is initialized(when
     * the Web application is deployed). You can initialize servlet context
     * related data here.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            config.validate();
        } catch (ConfigurationValidationException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Invalid configuration! Reason: " + e.getMessage(), e);
        }
        mgr.register("test-executor").withMax(4).withCore(2).add();

        if (config.isMetricsCollectionEnabled()) {
            metricsService.registerDefaultCollectors();
            sce.getServletContext().addServlet("prom", new MetricsServlet()).addMapping("/metrics");
        }
        if (config.isJavaMelodyEnabled()) {
            sce.getServletContext().addServlet("javamelody", new ReportServlet()).addMapping("/monitoring");
        }
    }

    /**
     * This method is invoked when the Servlet Context (the Web application)
     * is undeployed or Application Server shuts down.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        // https://stackoverflow.com/questions/11872316/tomcat-guice-jdbc-memory-leak
        AbandonedConnectionCleanupThread.checkedShutdown();
        logger.info("AbandonedConnectionCleanupThread shut down.");

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        Driver d = null;
        while (drivers.hasMoreElements()) {
            try {
                d = drivers.nextElement();
                DriverManager.deregisterDriver(d);
                logger.info(String.format("Driver %s deregistered", d));
            } catch (SQLException ex) {
                logger.warn(String.format("Error deregistering driver %s", d), ex);
            }
        }

        // The ThreadPoolManager should be able to automatically stop the instances
    }
}
