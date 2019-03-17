package org.k3a.springboot.reloadvalue;

import org.k3a.springboot.reloadvalue.annotation.EnableValueReload;
import org.k3a.springboot.reloadvalue.beanporcessor.ValueReloadPostProcessor;
import org.k3a.springboot.reloadvalue.config.ConfigWrapper;
import org.k3a.springboot.reloadvalue.config.ReloadableConfig;
import org.k3a.springboot.reloadvalue.context.ReloadContext;
import org.k3a.springboot.reloadvalue.convert.ValueConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by k3a
 * on 2019/2/16  PM7:42
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnBean(annotation = EnableValueReload.class)
public class AutoConfigReloadValue {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigReloadValue.class);

    private final ApplicationContext context;

    public AutoConfigReloadValue(ApplicationContext context) {
        this.context = context;
        // create and inject
        final ReloadContext reloadContext = ReloadContext.initWith(context);
        ((GenericApplicationContext) context).registerBean("reloadContext", ReloadContext.class, () -> reloadContext);
    }

    @Bean
    @ConditionalOnMissingBean(ValueReloadPostProcessor.class)
    public ValueReloadPostProcessor valueReloadPostProcessor() {
        return new ValueReloadPostProcessor();
    }

    /**
     * get main properties of Spring and create Config
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean(ReloadableConfig.class)
    public ReloadableConfig reloadableConfig(@Autowired(required = false) @Qualifier("propsConverter") ValueConverter converter,
                                             @Autowired(required = false) @Qualifier("rvUpdateHandler") Map<String, Consumer<String>> updateHandler)
            throws InterruptedException {
        final MutablePropertySources propertySources = ((ConfigurableEnvironment) context.getEnvironment()).getPropertySources();
        for (PropertySource<?> source : propertySources) {
            if (source instanceof OriginTrackedMapPropertySource) {
                final String name = source.getName();
                if (name.startsWith("applicationConfig: [file:") && name.endsWith("]")) {
                    //get name of the config file
                    final String configPath = name.substring(25, name.length() - 1);
                    LOGGER.info("choose {} as config file", configPath);
                    final boolean addSysProps = Boolean.parseBoolean(System.getProperty(ConfigWrapper.RV_ADD_SYS_PROPS, "true"));
                    final ReloadableConfig config = new ReloadableConfig(addSysProps, updateHandler, Paths.get(configPath), converter);
                    ReloadContext.getInstance().setConfig(config);
                    return config;
                }
            }
        }
        throw new IllegalStateException("no properties file was found");
    }

}
