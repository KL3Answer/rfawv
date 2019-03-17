package org.k3a.springboot.reloadvalue.beanporcessor;

import org.k3a.springboot.reloadvalue.context.ReloadContext;
import org.k3a.springboot.reloadvalue.dto.FieldSite;
import org.k3a.springboot.reloadvalue.utils.ReflectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by k3a
 * on 2019/1/29  PM 2:24
 */
@SuppressWarnings("WeakerAccess")
public class ValueReloadPostProcessor implements BeanPostProcessor {

    public final LongAdder needInitSum = new LongAdder();
    public final LongAdder initFinSum = new LongAdder();

    private final ThreadPoolExecutor executor;

    public ValueReloadPostProcessor() {
        final int coreSize = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(coreSize, coreSize, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100_000));
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
        needInitSum.increment();
        final ReloadContext reloadContext = ReloadContext.getInstance();
        executor.execute(() -> {
            // get real class defined in source code
            final Class<?> clazz = ClassUtils.getUserClass(bean.getClass());
            // handle @Value
            // exclude @PropertySource
            if (clazz.getAnnotation(PropertySource.class) == null) {
                ReflectionUtils.handleFields(clazz, field -> {
                    //check annotation
                    final Value value = field.getAnnotation(Value.class);
                    if (value == null) {
                        return;
                    }
                    // handle SpEL and placeHolder
                    final String v = value.value();
                    if ((v.startsWith(SystemPropertyUtils.PLACEHOLDER_PREFIX) && v.endsWith(SystemPropertyUtils.PLACEHOLDER_SUFFIX)) ||
                            (v.startsWith(StandardBeanExpressionResolver.DEFAULT_EXPRESSION_PREFIX) && v.endsWith(StandardBeanExpressionResolver.DEFAULT_EXPRESSION_SUFFIX))) {
                        reloadContext.valueFieldSites.add(new FieldSite(bean, field, v));
                    }
                });
            }
            // fin count
            initFinSum.increment();
        });
        return bean;
    }

    /**
     * make sure that all @Value fields are handled
     */
    public void awaitAndShutDown() {
        try {
            final long start = System.currentTimeMillis();
            while (initFinSum.sum() != needInitSum.sum()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignore) {
                }
                //超时控制，默认3分钟
                if (System.currentTimeMillis() - start > 3 * 60 * 1000) {
                    throw new IllegalStateException("waiting gtBeanPostProcessor timeout");
                }
            }
        } finally {
            executor.shutdown();
        }
    }

}
