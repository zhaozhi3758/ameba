package conf

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import org.apache.commons.lang3.StringUtils

import static ch.qos.logback.classic.Level.*

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}


Properties properties = context.getObject("properties");
String trace = properties.getProperty("ameba.trace.enabled");
boolean isTrace = "true".equalsIgnoreCase(trace);
String appPackage = properties.getProperty("app.package");

logger("org.glassfish", isTrace ? TRACE : WARN)
logger("org.glassfish.jersey.filter", INFO)
logger("org.glassfish.jersey.message.internal", isTrace ? TRACE : OFF)
logger("org.glassfish.jersey.server.ServerRuntime\$Responder", isTrace ? TRACE : OFF)

logger("org.avaje.ebean.SQL", TRACE)
logger("org.avaje.ebean.TXN", TRACE)
logger("org.avaje.ebean.SUM", TRACE)
logger("org.avaje.ebean.cache.QUERY", TRACE)
logger("org.avaje.ebean.cache.BEAN", TRACE)
logger("org.avaje.ebean.cache.COLL", TRACE)
logger("org.avaje.ebean.cache.NATKEY", TRACE)
logger("com.avaje.ebeaninternal.server.lib.sql", isTrace ? TRACE : DEBUG)
logger("com.avaje.ebeaninternal.server.transaction", isTrace ? TRACE : DEBUG)
logger("com.avaje.ebeaninternal.server.cluster", DEBUG)
logger("com.avaje.ebeaninternal.server.lib", DEBUG)
logger("com.avaje.ebeaninternal.server.deploy.BeanDescriptor", isTrace ? TRACE : DEBUG)
logger("com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager", WARN)

logger("ameba", isTrace ? TRACE : DEBUG)

if (appPackage != null) {
    for (String pkg : appPackage.split(",")) {
        if (StringUtils.isNotBlank(pkg))
            logger(pkg, TRACE)
    }
}

root(WARN, ["CONSOLE"])