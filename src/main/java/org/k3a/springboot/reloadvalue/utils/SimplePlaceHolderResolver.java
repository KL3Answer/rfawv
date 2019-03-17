package org.k3a.springboot.reloadvalue.utils;

import java.util.*;
import java.util.function.Function;

/**
 * Created by k3a
 * on 2019/1/30  PM 9:00
 */
public class SimplePlaceHolderResolver {

    public static final SimplePlaceHolderResolver DEFAULT = new SimplePlaceHolderResolver("${", "}");

    private final String startChars;
    private final String endChars;

    public SimplePlaceHolderResolver(String startChars, String endChars) {
        this.startChars = startChars;
        this.endChars = endChars;
    }

    /**
     * resolve placeHolder
     */
    public String resolvePropertiesPlaceHolder(ConcurrentProps concurrentProps, final String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        final int[] pair = get1stPair(startChars, endChars, key);
        //return origin key when placeHolder pair not found
        if (pair[0] == -1 || pair[1] == -1) {
            return key;
        }

        final String pre = key.substring(0, pair[0]);
        final String replacement = resolvePropertiesPlaceHolder(concurrentProps, key.substring(pair[0] + startChars.length(), pair[1]));
        final String mid = concurrentProps.getProperty(replacement);
        if (mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("malformed placeHolder value:" + startChars + replacement + endChars);
        }

        return pre + mid + resolvePropertiesPlaceHolder(concurrentProps, key.substring(pair[1] + endChars.length()));
    }

    public String resolveGenericPlaceHolder(Map<?, ?> map, final String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        final int[] pair = get1stPair(startChars, endChars, key);
        //return origin key when placeHolder pair not found
        if (pair[0] == -1 || pair[1] == -1) {
            return key;
        }

        final String pre = key.substring(0, pair[0]);
        final String replacement = resolveGenericPlaceHolder(map, key.substring(pair[0] + startChars.length(), pair[1]));
        final String mid = String.valueOf(map.get(replacement));
        if (mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("malformed placeHolder value:" + startChars + replacement + endChars);
        }

        return pre + mid + resolveGenericPlaceHolder(map, key.substring(pair[1] + endChars.length()));
    }

    /**
     * resolve placeHolder
     */
    public String resolvePlaceHolder(Map<String, String> map, final String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        final int[] pair = get1stPair(startChars, endChars, key);
        //return origin key when placeHolder pair not found
        if (pair[0] == -1 || pair[1] == -1) {
            return key;
        }

        final String pre = key.substring(0, pair[0]);
        final String replacement = resolvePlaceHolder(map, key.substring(pair[0] + startChars.length(), pair[1]));
        final String mid = map.get(replacement);
        if (mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("malformed placeHolder value:" + startChars + replacement + endChars);
        }

        return pre + mid + resolvePlaceHolder(map, key.substring(pair[1] + endChars.length()));
    }

    /**
     * find l pair
     */
    public static int[] get1stPair(final String start, final String end, final String text) {
        int[] arr = {-1, -1};
        final Stack<Integer> startIndex = new Stack<>();

        for (int i = 0; i <= text.length(); i++) {
            if (i - start.length() >= 0 && text.substring(i - start.length(), i).equals(start)) {
                startIndex.push(i - start.length());
            } else if (i - end.length() >= 0 && text.substring(i - end.length(), i).equals(end)) {
                if (startIndex.size() > 0) {
                    final Integer pop = startIndex.pop();
                    if (pop != null) {
                        if (startIndex.empty()) {
                            return new int[]{pop, i - end.length()};
                        } else {
                            arr = new int[]{pop, i - end.length()};
                        }
                    }
                }
            }
        }
        return arr;
    }

    /**
     * e.g.
     * <p>
     * a=1
     * b=2
     * c=3
     * <p>
     * ${a${b}}${c} -> [a2,b,c]
     */
    public static Set<String> getAllPlaceHolders(String start, String end, String text, Function<String, String> resolver) {
        final HashSet<String> all = new HashSet<>();
        final int[] pair = get1stPair(start, end, text);
        if (pair[0] != -1 && pair[1] != -1) {
            final String substring = text.substring(pair[0] + start.length(), pair[1]);
            all.add(resolver.apply(substring));
            all.addAll(getAllPlaceHolders(start, end, substring, resolver));
            if (pair[1] + end.length() < text.length()) {
                all.addAll(getAllPlaceHolders(start, end, text.substring(pair[1] + end.length()), resolver));
            }
        }
        return all;
    }

}