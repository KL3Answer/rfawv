package org.k3a.springboot.reloadvalue.config;

import org.k3a.springboot.reloadvalue.context.ReloadContext;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * Created by k3a
 * on 2019/2/13  PM 4:49
 *
 * @noinspection WeakerAccess
 */
public class ReloadablePropertyResolver extends AbstractPropertyResolver {

    protected final MutablePropertySources propertySources;

    /**
     * Create a new resolver against the given property sources.
     */
    public ReloadablePropertyResolver() {
        final MutablePropertySources propertySources = new MutablePropertySources();
        //remove configurationProperties(it will cache old properties and hide the change of origin props)
        ((ConfigurableEnvironment) ReloadContext.getInstance().getContext().getEnvironment()).getPropertySources()
                .stream()
                .filter(e -> !e.getName().equals("configurationProperties"))
                .forEach(propertySources::addFirst);

        this.propertySources = propertySources;
    }

    @Override
    public boolean containsProperty(String key) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (propertySource.containsProperty(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Nullable
    public String getProperty(String key) {
        return getProperty(key, String.class, true);
    }

    @Override
    @Nullable
    public <T> T getProperty(String key, Class<T> targetValueType) {
        return getProperty(key, targetValueType, true);
    }

    @Override
    @Nullable
    protected String getPropertyAsRawString(String key) {
        return getProperty(key, String.class, false);
    }

    @Nullable
    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Searching for key '" + key + "' in PropertySource '" +
                            propertySource.getName() + "'");
                }
                Object value = propertySource.getProperty(key);
                if (value != null) {
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = resolveNestedPlaceholders((String) value);
                    }
                    logKeyFound(key, propertySource, value);
                    return convertValueIfNecessary(value, targetValueType);
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Could not find key '" + key + "' in any property config");
        }
        return null;
    }

    /**
     * Log the given key as found in the given {@link PropertySource}, resulting in
     * the given value.
     * <p>The default implementation writes a debug log message with key and config.
     * As of 4.3.3, this does not log the value anymore in order to avoid accidental
     * logging of sensitive settings. Subclasses may override this method to change
     * the log level and/or log message, including the property's value if desired.
     *
     * @param key            the key found
     * @param propertySource the {@code PropertySource} that the key has been found in
     * @param value          the corresponding value
     * @since 4.3.1
     */
    protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
                    "' with value of type " + value.getClass().getSimpleName());
        }
    }

    public MutablePropertySources getPropertySources() {
        return propertySources;
    }
}
