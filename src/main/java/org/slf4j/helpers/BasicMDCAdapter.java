package org.slf4j.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.spi.MDCAdapter;

public class BasicMDCAdapter implements MDCAdapter {
    private InheritableThreadLocal<Map<String, String>> inheritableThreadLocal = new InheritableThreadLocal<Map<String, String>>() {
        protected Map<String, String> childValue(Map<String, String> parentValue) {
            return parentValue == null ? null : new HashMap<>(parentValue);
        }
    };

    @Override
    public void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        Map<String, String> map = this.inheritableThreadLocal.get();
        if (map == null) {
            map = new HashMap<>();
            this.inheritableThreadLocal.set(map);
        }

        map.put(key, val);
    }

    @Override
    public String get(String key) {
        Map<String, String> map = this.inheritableThreadLocal.get();
        return map != null && key != null ? map.get(key) : null;
    }

    @Override
    public void remove(String key) {
        Map<String, String> map = this.inheritableThreadLocal.get();
        if (map != null) {
            map.remove(key);
        }
    }

    @Override
    public void clear() {
        Map<String, String> map = this.inheritableThreadLocal.get();
        if (map != null) {
            map.clear();
            this.inheritableThreadLocal.remove();
        }
    }

    public Set<String> getKeys() {
        Map<String, String> map = this.inheritableThreadLocal.get();
        return map != null ? map.keySet() : null;
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        Map<String, String> oldMap = this.inheritableThreadLocal.get();
        return oldMap != null ? new HashMap<>(oldMap) : null;
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        this.inheritableThreadLocal.set(new HashMap<>(contextMap));
    }
}
