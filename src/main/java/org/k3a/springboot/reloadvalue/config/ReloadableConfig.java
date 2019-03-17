package org.k3a.springboot.reloadvalue.config;

import org.k3a.observer.Observer;
import org.k3a.observer.impl.FileObserver;
import org.k3a.springboot.reloadvalue.context.ReloadContext;
import org.k3a.springboot.reloadvalue.convert.SpringValueConverter;
import org.k3a.springboot.reloadvalue.convert.ValueConverter;
import org.k3a.springboot.reloadvalue.dto.FieldSite;
import org.k3a.springboot.reloadvalue.utils.ConcurrentProps;
import org.k3a.springboot.reloadvalue.utils.Pair;
import org.k3a.springboot.reloadvalue.utils.SimplePlaceHolderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.SystemPropertyUtils;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.stream.Collectors.*;

/**
 * Created by k3a
 * on 2019/2/19  PM 6:44
 */
@SuppressWarnings("WeakerAccess")
public class ReloadableConfig implements Config<ConcurrentProps>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadableConfig.class);

    // control group
    protected volatile ConcurrentProps props;

    // copy of keys of properties load from file
    protected volatile Set<Object> oldKeys;

    protected volatile Map<String, Consumer<String>> updateHandler = new ConcurrentHashMap<>();

    // l -> valueEx ,r -> fieldSite
    protected volatile Map<Pair<String, FieldSite>, Consumer<String>> valueReloadHandler = new ConcurrentHashMap<>();

    protected final ReloadablePropertyResolver propertyResolver = new ReloadablePropertyResolver();

    protected final String ignoreResolvedStr = SystemPropertyUtils.PLACEHOLDER_PREFIX + "random.";

    public final boolean addSysProps;

    protected volatile Observer<Path, WatchService> register;

    protected final Path path;

    protected final ValueConverter converter;

    protected FileTime lastModifiedTime;

    public ReloadableConfig(boolean addSysProps, Map<String, Consumer<String>> updateHandler, final Path path, ValueConverter converter) throws InterruptedException {
        this.addSysProps = addSysProps;
        this.path = path;
        if (updateHandler != null) {
            this.updateHandler.putAll(updateHandler);
        }

        init();
        startWatching();

        if (converter != null) {
            this.converter = converter;
        } else {
            this.converter = new SpringValueConverter(ReloadContext.getInstance().conversionService);
        }
    }

    @Override
    public ConcurrentProps getSource() {
        return props;
    }

    @Override
    public String getString(String key) {
        return props.getProperty(key);
    }

    @Override
    public void close() throws Exception {
        register.stop();
    }

    @Override
    public String resolve(String text) {
        try {
            return propertyResolver.resolveRequiredPlaceholders(text);
        } catch (Exception e) {
            LOGGER.error("resolve placeHolder failed", e);
            return null;
        }
    }

    /**
     * key -> key in properties,value -> updateHandler
     */
    public void registerUpdateHandler(String key, Consumer<String> handler) {
        updateHandler.put(key, handler);
    }

    /**
     * reload @Value
     */
    public void registerValueReload(Pair<String, FieldSite> pair, Consumer<String> handler) {
        valueReloadHandler.put(pair, handler);
    }

    /**
     * load config file first
     */
    protected void init() {
        try {
            //get properties loaded first
            final ConcurrentProps props = loadProperties(path, addSysProps ? prop -> {
                // as a copy
                oldKeys = new HashSet<>(prop.keySet());
                //resolve placeHolders
                // notice that placeHolders like ${random.XXX} injected will change while reload
                prop.forEach((k, v) -> prop.setProperty((String) k, resolve((String) v)));
                System.getProperties().putAll(prop);
                prop.putAll(System.getProperties());
            } : prop -> {
                // as a copy
                oldKeys = new HashSet<>(prop.keySet());
                prop.forEach((k, v) -> prop.setProperty((String) k, resolve((String) v)));
            });

            lastModifiedTime = Files.getLastModifiedTime(path);

            if (addSysProps) {
                props.putAll(System.getProperties());
                System.getProperties().putAll(props);
            }
            this.props = props;

        } catch (Exception e) {
            throw new IllegalStateException("ReloadableConfig loadActions failed", e);
        }
    }

    /**
     * object config file
     */
    protected void startWatching() throws InterruptedException {
        register = FileObserver.get().register(path);
        if (addSysProps) {
            props.putAll(System.getProperties());
            System.getProperties().putAll(props);
        }
        register.onModify(this::onUpdate);
        register.start();
    }

    /**
     * update config after config file modified
     */
    protected void onUpdate(Path p) {
        try {
            final ConcurrentProps load = loadProperties(p, prop -> {
                // update resolver
                final MutablePropertySources propertySources = propertyResolver.getPropertySources();
                String name;
                for (PropertySource<?> source : propertySources) {
                    if (source instanceof OriginTrackedMapPropertySource
                            && (name = source.getName()).startsWith("applicationConfig: [file:") && name.endsWith("]")
                            && p.toString().equals(name.substring(25, name.length() - 1))) {
                        propertySources.replace(name, new OriginTrackedMapPropertySource(name, prop));
                        break;
                    }
                }
                // resolve placeHolder(ignore randoms)
                prop.forEach((k, v) -> {
                    if (((String) v).startsWith(ignoreResolvedStr)) {
                        return;
                    }
                    prop.setProperty((String) k, resolve((String) v));
                });
            });

            // 0.0 clear deleted pairs which are stored in system properties
            oldKeys.removeAll(load.keySet());
            for (Object o : oldKeys) {
                System.getProperties().remove(o);
            }
            oldKeys = new HashSet<>(load.keySet());

            // 1.0 reloadValues
            reloadValues(load);
            // 2.0 invoke customized update handler
            updateHandler.forEach((k, v) -> {
                try {
                    final String value = load.getProperty(k);
                    if (value != null && !value.equals(props.getProperty(k))) {
                        v.accept(value);
                    }
                } catch (Exception e) {
                    LOGGER.error("updateHandler error", e);
                }
            });
            // 3.0 reset
            ReloadContext.getInstance().valueFieldSites.forEach(e -> e.executed.set(false));
            // 4.0 update props and add to system properties if needed
            if (addSysProps) {
                System.getProperties().putAll(load);
                load.putAll(System.getProperties());
            }
            props = load;
        } finally {
            LOGGER.info("config :{} updated at {}", p, System.currentTimeMillis());
        }
    }

    /**
     * reload fields annotated with @Value
     */
    protected void reloadValues(ConcurrentProps load) {

        final Set<Pair<String, FieldSite>> newValue = new HashSet<>();
        final Set<Pair<String, FieldSite>> unusedValue = new HashSet<>();

        // pre-handle ，help comparing and get unused valueEX; key -> field ,value -> value in @Value
        final Map<FieldSite, Set<String>> fvSet = valueReloadHandler.keySet().stream()
                .collect(groupingBy(e -> e.r, mapping(e -> e.l, toSet())));

        // invoke reload
        valueReloadHandler.forEach((k, v) -> {
            final String value = load.getProperty(k.l);
            if (value == null && props.getProperty(k.l) != null) {
                // try to evaluate default value
                v.accept(k.r.valueEx);
            } else if (value != null && !value.equals(props.getProperty(k.l)) && !value.startsWith(ignoreResolvedStr)) {//reload only when updated and non-random
                v.accept(k.r.valueEx);

                final Set<String> placeHolders = SimplePlaceHolderResolver
                        .getAllPlaceHolders(SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX, k.r.valueEx, this::resolve)
                        .stream().map(e -> {
                            final int i = e.indexOf(SystemPropertyUtils.VALUE_SEPARATOR);
                            return i == -1 ? e.trim() : e.substring(0, i).trim();
                        }).collect(toSet());
                // register new nested valueEX
                placeHolders.forEach(e -> {
                    final Pair<String, FieldSite> pair = new Pair<>(e, k.r);
                    if (!valueReloadHandler.keySet().contains(pair)) {
                        newValue.add(pair);
                    }
                });
                // record unused valueEX
                fvSet.get(k.r).forEach(e -> {
                    if (!placeHolders.contains(e)) {
                        unusedValue.add(new Pair<>(e, k.r));
                    }
                });
            }
        });

        // register new value
        newValue.forEach(e -> registerValueReload(e, v -> ReloadContext.getInstance().reload.accept(v, e.r)));
        // del unused handlers(only remove nested placeHolders)
        unusedValue.forEach(e -> valueReloadHandler.remove(e));
    }

    protected ConcurrentProps loadProperties(final Path p, final Consumer<ConcurrentProps> consumer) {
        try (FileInputStream fis = new FileInputStream(p.toFile())) {
            final ConcurrentProps prop = new ConcurrentProps();
            prop.load(fis);
            if (consumer != null) {
                consumer.accept(prop);
            }
            return prop;
        } catch (Exception e) {
            LOGGER.error("loadPropertiesError", e);
        }
        return null;
    }

    public Path getPath() {
        return path;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public ValueConverter getConverter() {
        return converter;
    }
}
