package org.k3a.springboot.reloadvalue.listener;

import org.k3a.springboot.reloadvalue.beanporcessor.ValueReloadPostProcessor;
import org.k3a.springboot.reloadvalue.context.ReloadContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Created by k3a
 * on 2019/1/28  PM 4:22
 * <p>
 * wait util ValueReloadPostProcessor finish or time out
 */
public class ReloadValueApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        event.getApplicationContext().getBean(ValueReloadPostProcessor.class).awaitAndShutDown();
        ReloadContext.getInstance().start();
    }

}
