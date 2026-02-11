package net.bteuk.network.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class BukkitForwardingHandler extends Handler {

    private final Logger pluginLogger;
    private final String pluginLoggerName;

    public BukkitForwardingHandler(Logger pluginLogger) {
        this.pluginLogger = pluginLogger;
        this.pluginLoggerName = pluginLogger.getName();
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        // Avoid feedback loops if someone already uses the plugin logger
        String src = record.getLoggerName();
        if (src != null && src.equals(pluginLoggerName)) {
            return;
        }

        // Delegate to the plugin logger so its prefix/formatting is applied
        pluginLogger.log(record);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
