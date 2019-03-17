package org.k3a.springboot.reloadvalue.config;


import org.k3a.springboot.reloadvalue.utils.SimplePlaceHolderResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by k3a
 * on 2019/1/22  AM 9:45
 */
public interface Config<T> {

    T getSource();

    void registerUpdateHandler(String key, Consumer<String> handler);

    String getString(String key);

    default String getString(String key, String defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    default Boolean getBoolean(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default Byte getByte(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Byte.parseByte(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default byte getByte(String key, byte defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Byte.parseByte(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default Integer getInt(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default int getInt(String key, int defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default Long getLong(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default long getLong(String key, long defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default Float getFloat(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default float getFloat(String key, float defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default Double getDouble(String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        } else {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    default double getDouble(String key, double defaultValue) {
        final String value = getString(key);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    default String resolve(String value) {
        final Type[] genericInterfaces = this.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (((ParameterizedType) type).getRawType().equals(Config.class)
                    && Map.class.isAssignableFrom((Class) ((ParameterizedType) type).getActualTypeArguments()[0])) {
                return SimplePlaceHolderResolver.DEFAULT.resolveGenericPlaceHolder((Map) this.getSource(), value);
            }

        }
        return null;
    }

}
