package org.k3a.springboot.reloadvalue.utils;

/**
 * Created by k3a
 * on 19-1-3  AM 9:56
 */
public class SimpleStringUtils {


    public static String join(String separator, String... str) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (separator == null) {
            separator = "";
        }

        final StringBuilder tmp = new StringBuilder();
        tmp.append(str[0]);
        for (int i = 1; i < str.length; i++) {
            tmp.append(separator).append(str[1]);
        }
        return tmp.toString();
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static String[] trim(String arr[]) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].trim();
        }
        return arr;
    }

}
