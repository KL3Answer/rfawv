package org.k3a.springboot.reloadvalue.config;

import org.k3a.springboot.reloadvalue.utils.ConcurrentProps;

import java.util.function.Consumer;

/**
 * Created by k3a
 * on 2019/1/23  PM 2:10
 * <p>
 * extend this class and then you can access config through these predefined method like getString ,getBoolean ,get .etc
 */
@SuppressWarnings({"unused"})
public abstract class ConfigWrapper implements Config<ConcurrentProps> {

    //keys of config file
    public static final String RV_ADD_SYS_PROPS = "reloadvalue.addSysProps";
    //#end

    public final ReloadableConfig config;

    /**
     * inject by @Autowired or create on you own
     */
    public ConfigWrapper(ReloadableConfig config) {
        this.config = config;
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public void registerUpdateHandler(String key, Consumer<String> handler) {
        config.registerUpdateHandler(key, handler);
    }

    @Override
    public ConcurrentProps getSource() {
        return config.getSource();
    }

    /**
     * common converter
     */
    public <T> T get(Class<T> type, String text) {
        return config.getConverter().convert(type, text);
    }

}
