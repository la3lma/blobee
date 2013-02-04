package no.rmz.maputils;

import static com.google.common.base.Preconditions.checkNotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class InstrumentedHashMap<A, B> implements Map<A, B> {
    private final String name;
    private final Object creator;

    private static final Logger log = Logger.getLogger(InstrumentedHashMap.class.getName());
    private final Map<A, B> backend;

    public InstrumentedHashMap(final Object creator, final String name) {
        this(creator, name, new HashMap<A, B>());
    }

    public InstrumentedHashMap(final Object clazz, final String name, final Map<A, B> backend) {
        this.creator = checkNotNull(clazz);
        this.name = checkNotNull(name);
        this.backend = checkNotNull(backend);
        InstrumentedMapRegistry.register(this);
    }

    public String getName() {
        return name;
    }

    public Object getCreator() {
        return creator;
    }



    public void log() {
             InstrumentedMapRegistry.log(this, size());
    }



    public int size() {
        return backend.size();
    }

    public boolean isEmpty() {
        return backend.isEmpty();
    }

    public boolean containsKey(Object key) {
        return backend.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return backend.containsValue(value);
    }

    public B get(Object key) {
        return backend.get(key);
    }

    public B put(A key, B value) {
        final B result = backend.put(key, value);
        log();
        return result;
    }

    public B remove(Object key) {
        final B result = backend.remove(key);
        log();
        return result;
    }

    public void putAll(Map<? extends A, ? extends B> m) {
        backend.putAll(m);
    }

    public void clear() {
        backend.clear();
    }

    public Set<A> keySet() {
        return backend.keySet();
    }

    public Collection<B> values() {
        return backend.values();
    }

    public Set<Entry<A, B>> entrySet() {
        return backend.entrySet();
    }

    public boolean equals(Object o) {
        return backend.equals(o);
    }

    public int hashCode() {
        return backend.hashCode();
    }

}
