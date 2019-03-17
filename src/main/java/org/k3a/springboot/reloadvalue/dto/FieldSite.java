package org.k3a.springboot.reloadvalue.dto;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by k3a
 * on 2019/1/29  AM 11:40
 */
public class FieldSite {

    public final Object owner;

    public final Field field;

    public final String valueEx;

    public final AtomicBoolean executed = new AtomicBoolean(false);

    public FieldSite(Object owner, Field field, String valueEx) {
        this.owner = owner;
        this.field = field;
        this.valueEx = valueEx;
        this.field.setAccessible(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldSite fieldSite = (FieldSite) o;

        if (!Objects.equals(owner, fieldSite.owner)) return false;
        if (!Objects.equals(field, fieldSite.field)) return false;
        return Objects.equals(valueEx, fieldSite.valueEx);
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (field != null ? field.hashCode() : 0);
        result = 31 * result + (valueEx != null ? valueEx.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FieldSite{" +
                "owner=" + owner +
                ", field=" + field +
                ", valueEx='" + valueEx + '\'' +
                '}';
    }
}