package com.ixale.starparse.gui;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

@Plugin(name = "StatusBar", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class StatusBarAppender extends AbstractAppender {

    public interface Listener {
        void onMessage(String msg, LogEvent event);
    }

    static Listener listener = null;

    public static void setListener(Listener l) {
        listener = l;
    }

    protected StatusBarAppender(String name, Filter filter, PatternLayout layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static StatusBarAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("PatternLayout") PatternLayout layout,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        
        if (name == null) {
            LOGGER.error("No name provided for StatusBarAppender");
            return null;
        }
        
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        
        return new StatusBarAppender(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event) {
        if (listener == null) {
            return;
        }
        listener.onMessage(this.getLayout().toSerializable(event).toString(), event);
    }
}
