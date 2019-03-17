package org.k3a.springboot.reloadvalue.convert;

/**
 * Created by k3a
 * on 3/17/19  12:38 PM
 */
public interface ValueConverter {
    <T> T convert(Class<T> type, String text);
}
