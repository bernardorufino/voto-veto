package com.votoxveto.app.helpers;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Table;

import java.util.Map;

public class CollectionsHelper {

    public static <R, C, V> V getOrDefault(Table<R, C, V> table, R row, C column, Supplier<? extends V> supplier) {
        V value = table.get(row, column);
        if (value == null) {
            value = supplier.get();
            table.put(row, column, value);
        }
        return value;
    }

    public static <K, V> V getOrDefault(Map<K, V> map, K key, Supplier<? extends V> supplier) {
        V value = map.get(key);
        if (value == null) {
            value = supplier.get();
            map.put(key, value);
        }
        return value;
    }

    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        return getOrDefault(map, key, Suppliers.ofInstance(defaultValue));
    }

    public static <R, C, V> V getOrDefault(Table< R, C, V> table, R row, C column, V defaultValue) {
        return getOrDefault(table, row, column, Suppliers.ofInstance(defaultValue));
    }

    public static <T> Optional<T> tryGetOther(T item, Iterable<? extends T> values) {
        for (T value : values) {
            if (item.equals(value)) continue;
            return Optional.of(value);
        }
        return Optional.absent();
    }

    public static <T> T getOther(T item, Iterable<? extends T> values) {
        return tryGetOther(item, values).get();
    }

    // Prevents instantiation
    private CollectionsHelper() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
