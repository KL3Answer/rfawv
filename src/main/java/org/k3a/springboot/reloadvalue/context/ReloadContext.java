package org.k3a.springboot.reloadvalue.context;

import org.k3a.springboot.reloadvalue.config.Config;
import org.k3a.springboot.reloadvalue.config.ReloadableConfig;
import org.k3a.springboot.reloadvalue.dto.FieldSite;
import org.k3a.springboot.reloadvalue.utils.Pair;
import org.k3a.springboot.reloadvalue.utils.SimplePlaceHolderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.SystemPropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by k3a
 * on 2019/1/23  PM 9:41
 * <p>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ReloadContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadContext.class);

    private static ReloadContext instance;

    protected Config<?> config;
    protected ApplicationContext context;

    public final List<FieldSite> valueFieldSites = new ArrayList<>();

    private AtomicBoolean running = new AtomicBoolean(false);

    public final BeanExpressionResolver spelResolver;

    public final BeanExpressionContext expressionContext;

    public final ConversionService conversionService;

    public final BiConsumer<String, FieldSite> reload;

    /**
     * must init before getInstance and do something else
     */
    public static ReloadContext initWith(ApplicationContext context) {
        if (instance == null) {
            synchronized (ReloadContext.class) {
                if (instance == null) {
                    instance = new ReloadContext(context);
                }
            }
        }
        return instance;
    }

    public static ReloadContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ReloadContext must init before use");
        }
        return instance;
    }

    private ReloadContext(ApplicationContext context) {
        this.context = context;

        final ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) this.context).getBeanFactory();
        spelResolver = beanFactory.getBeanExpressionResolver();
        conversionService = beanFactory.getConversionService();

        Objects.requireNonNull(spelResolver);
        Objects.requireNonNull(conversionService);

        expressionContext = new BeanExpressionContext(beanFactory, null);
        // inject into Spring context
        ((GenericApplicationContext) context).registerBean("spelResolver", BeanExpressionResolver.class, () -> spelResolver);
        ((GenericApplicationContext) context).registerBean("expressionContext", BeanExpressionContext.class, () -> expressionContext);

        reload = (v, fs) -> {
            // 0.0 check status and avoid reload field multi times in a round
            if (!fs.executed.compareAndSet(false, true)) {
                return;
            }
            // 1.0 resolve place holder
            final String resolve = config.resolve(v).trim();
            // 2.0 handle SpEL
            final Object rs = spelResolver.evaluate(resolve, expressionContext);
            // 3.0 convert and set
            final Class<?> type = fs.field.getType();
            try {
                if (type.isInstance(rs)) {
                    fs.field.set(fs.owner, rs);
                } else if (conversionService.canConvert(String.class, type)) {
                    fs.field.set(fs.owner, conversionService.convert(rs, type));
                } else {
                    LOGGER.warn("convert error ,type{},result{}", type, rs);
                }
            } catch (Exception e) {
                LOGGER.error("update value failed on:" + fs);
            }
        };
    }

    public void setConfig(Config<?> config) {
        this.config = config;
    }

    public Config<?> getConfig() {
        return config;
    }

    public ApplicationContext getContext() {
        return context;
    }

    /**
     * start watching config file
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            Objects.requireNonNull(config, "config must be set before start");
            try {
                valueReloadIfNeeded();
            } catch (Exception e) {
                throw new RuntimeException("start ReadContext failed", e);
            }
            LOGGER.info("ReadContext starting");
        }
    }

    public void stop() {
        if (config instanceof AutoCloseable) {
            try {
                ((AutoCloseable) config).close();
            } catch (Exception e) {
                LOGGER.error("stop readContext error", e);
            }
        }
    }

    /**
     * reload fields injected by @Value
     */
    protected void valueReloadIfNeeded() {
        if (!(config instanceof ReloadableConfig)) {
            return;
        }
        for (FieldSite efs : valueFieldSites) {
            // get all place holder
            SimplePlaceHolderResolver
                    .getAllPlaceHolders(SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX, efs.valueEx, e -> config.resolve(e))
                    .forEach(e -> {
                        final int i = e.indexOf(SystemPropertyUtils.VALUE_SEPARATOR);
                        ((ReloadableConfig) config)
                                //trim default value
                                //register reload handlers for every placeHolder
                                .registerValueReload(new Pair<>(config.resolve(i == -1 ? e.trim() : e.substring(0, i).trim()), efs), r -> reload.accept(r, efs));
                    });
        }
    }

}