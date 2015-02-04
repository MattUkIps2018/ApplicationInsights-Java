/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.channel.TelemetryChannel;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

import com.google.common.base.Strings;

/**
 * Encapsulates the global telemetry configuration typically loaded from the ApplicationInsights.xml file.
 *
 * All {@link com.microsoft.applicationinsights.telemetry.TelemetryContext} objects are initialized using the
 * 'Active' (returned by the 'getActive' static method) telemetry configuration provided by this class.
 */
public final class TelemetryConfiguration {

    // Synchronization for instance initialization
    private final static Object s_lock = new Object();
    private static volatile boolean initialized = false;
    private static TelemetryConfiguration active;

    private String instrumentationKey;

    private final ArrayList<ContextInitializer> contextInitializers = new ArrayList<ContextInitializer>();
    private final ArrayList<TelemetryInitializer> telemetryInitializers = new ArrayList<TelemetryInitializer>();
    private final ArrayList<TelemetryModule> telemetryModules = new ArrayList<TelemetryModule>();

    private TelemetryChannel channel;

    private boolean trackingIsDisabled = false;

    /**
     * Gets the active {@link com.microsoft.applicationinsights.TelemetryConfiguration} instance loaded from the
     * ApplicationInsights.xml file. If the configuration file does not exist, the active configuration instance is
     * initialized with minimum defaults needed to send telemetry to Application Insights.
     * @return The 'Active' instance
     */
    public static TelemetryConfiguration getActive() {
        if (!initialized) {
            synchronized (s_lock) {
                if (!initialized) {
                    active = new TelemetryConfiguration();
                    TelemetryConfigurationFactory.INSTANCE.initialize(active);
                    initialized = true;
                }
            }
        }

        return active;
    }

    /**
     * Creates a new instance loaded from the ApplicationInsights.xml file.
     * If the configuration file does not exist, the new configuration instance is initialized with minimum defaults
     * needed to send telemetry to Application Insights.
     * @return Telemetry Configuration instance.
     */
    public static TelemetryConfiguration createDefault() {
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration();
        TelemetryConfigurationFactory.INSTANCE.initialize(telemetryConfiguration);
        return telemetryConfiguration;
    }

    /**
     * Gets the telemetry channel.
     * @return An instance of {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
     */
    public TelemetryChannel getChannel() {
        return channel;
    }

    /**
     * Sets the telemetry channel.
     * @param channel An instance of {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
     */
    public void setChannel(TelemetryChannel channel) {
        this.channel = channel;
    }

    /**
     * Gets value indicating whether sending of telemetry to Application Insights is disabled.
     *
     * This disable tracking setting value is used by default by all {@link com.microsoft.applicationinsights.TelemetryClient}
     * instances created in the application.
     *
     * @return True if tracking is disabled.
     */
    public boolean isTrackingDisabled() {
        return trackingIsDisabled;
    }

    /**
     * Sets value indicating whether sending of telemetry to Application Insights is disabled.
     *
     * This disable tracking setting value is used by default by all {@link com.microsoft.applicationinsights.TelemetryClient}
     * instances created in the application.
     * @param disable True to disable tracking.
     */
    public void setTrackingIsDisabled(boolean disable) {
        trackingIsDisabled = disable;
    }

    /**
     * Gets the list of {@link ContextInitializer} objects that supply additional information about application.
     *
     * Context initializers extend Application Insights telemetry collection by supplying additional information
     * about application environment, such as 'User' information (in TelemetryContext.getUser or Device (in TelemetryContext.getDevice
     * invokes telemetry initializers each time the TelemetryClient's 'track' method is called
     *
     * The default list of telemetry initializers is provided by the SDK and can also be set from the ApplicationInsights.xml.
     * @return Collection of Context Initializers
     */
    public List<ContextInitializer> getContextInitializers() {
        return contextInitializers;
    }

    /**
     * Gets the list of modules that automatically generate application telemetry.
     *
     * Telemetry modules automatically send telemetry describing the application to Application Insights. For example, a telemetry
     * module can handle application exception events and automatically send
     * @return List of Telemetry Initializers
     */
    public List<TelemetryInitializer> getTelemetryInitializers() {
        return telemetryInitializers;
    }

    public List<TelemetryModule> getTelemetryModules() {
        return telemetryModules;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     *
     * This instrumentation key value is used by default by all {@link com.microsoft.applicationinsights.TelemetryClient}
     * instances created in the application. This value can be overwritten by setting the Instrumentation Key in
     * {@link com.microsoft.applicationinsights.telemetry.TelemetryContext} class
     * @return The instrumentation key
     */
    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     *
     * This instrumentation key value is used by default by all {@link com.microsoft.applicationinsights.TelemetryClient}
     * instances created in the application. This value can be overwritten by setting the Instrumentation Key in
     * {@link com.microsoft.applicationinsights.telemetry.TelemetryContext} class
     * @param key The instrumentation key
     * @throws IllegalArgumentException when the new value is null or empty
     */
    public void setInstrumentationKey(String key) {
        if (!Sanitizer.isUUID(key)) {
            InternalLogger.INSTANCE.trace("Telemetry Configuration: instrumentation key '%s' is not in UUID format", key);
        }

        // A non null, non empty instrumentation key is a must
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        instrumentationKey = key;
    }
}
