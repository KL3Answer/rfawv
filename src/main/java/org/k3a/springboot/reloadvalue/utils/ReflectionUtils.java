package org.k3a.springboot.reloadvalue.utils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by k3a
 * on 2019/2/19  PM 5:43
 */
public class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
     * 获取 objClazz 上真正标记 annotation 的父类、接口或它本身
     */
    public static Class<?> getAnnotatedClass(Class objClazz, Class annotation) {
        if (objClazz == null) {
            return null;
        }
        if (objClazz.getAnnotation(annotation) != null) {
            return objClazz;
        } else {
            Class<?> tmp;
            //interface l
            Class<?>[] interfaces = objClazz.getInterfaces();
            if (interfaces != null && interfaces.length > 0
                    && (tmp = getAnnotatedClass(objClazz.getInterfaces(), annotation)) != null) {
                return tmp;
            }
            //super class
            return getAnnotatedClass(objClazz.getSuperclass(), annotation);
        }
    }

    public static Class<?> getAnnotatedClass(Class[] interfaces, Class annotation) {
        if (interfaces == null || interfaces.length == 0) {
            return null;
        }
        Class<?> tmp;
        for (Class<?> anInterface : interfaces) {
            //noinspection unchecked
            if (anInterface.getAnnotation(annotation) != null) {
                return anInterface;
            } else {
                if ((tmp = getAnnotatedClass(anInterface.getInterfaces(), annotation)) != null) {
                    return tmp;
                }
            }
        }
        return null;
    }

    /**
     * 获取 clazz 上除了 JDK 部分类 之外的所有 父类的 field
     */
    public static Set<Field> getAllField(Class<?> clazz) {
        return getAllField(clazz, null);
    }

    /**
     * 获取 clazz 上除了 JDK 部分类 之外的所有 父类的 field
     */
    public static Set<Field> getAllField(Class<?> clazz, Predicate<Field> predicate) {
        if (clazz.getClassLoader() == null) {
            //noinspection unchecked
            return Collections.EMPTY_SET;
        }

        final Set<Field> fields = new HashSet<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (predicate == null || predicate.test(f)) {
                f.setAccessible(true);
                fields.add(f);
            }
        }
        fields.addAll(getAllField(clazz.getSuperclass(), predicate));
        return fields;
    }

    /**
     * 处理 clazz 所有field（除了 JDK 的部分类）
     */
    public static void handleFields(Class<?> clazz, Consumer<Field> consumer) {
        if (clazz.getClassLoader() == null) {
            return;
        }

        for (Field f : clazz.getDeclaredFields()) {
            consumer.accept(f);
        }
        handleFields(clazz.getSuperclass(), consumer);
    }

}
