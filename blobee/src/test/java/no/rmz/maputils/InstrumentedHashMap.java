package no.rmz.maputils;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A hash map that logs crea
 * @author rmz
 * @param <A>
 * @param <B>
 */
public final class InstrumentedHashMap<A, B> implements Map<A, B> {
    private final String name;
    private final Object creator;

    private static final Logger log =
            Logger.getLogger(InstrumentedHashMap.class.getName());
    private final Map<A, B> backend;

    public InstrumentedHashMap(final Object creator, final String name) {
        this(creator, name, new HashMap<A, B>());
    }

    public InstrumentedHashMap(
            final Object clazz,
            final String name,
            final Map<A, B> backend) {
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



    @Override
    public int size() {
        return backend.size();
    }

    @Override
    public boolean isEmpty() {
        return backend.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return backend.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return backend.containsValue(value);
    }

    @Override
    public B get(final Object key) {
        return backend.get(key);
    }

    @Override
    public B put(final A key, final B value) {
        final B result = backend.put(key, value);
        log();
        return result;
    }

    @Override
    public B remove(final Object key) {
        final B result = backend.remove(key);
        log();
        return result;
    }

    @Override
    public void putAll(final Map<? extends A, ? extends B> m) {
        backend.putAll(m);
    }

    @Override
    public void clear() {
        backend.clear();
    }

    @Override
    public Set<A> keySet() {
        return backend.keySet();
    }

    @Override
    public Collection<B> values() {
        return backend.values();
    }

    @Override
    public Set<Entry<A, B>> entrySet() {
        return backend.entrySet();
    }

    @Override
    public boolean equals(final Object o) {
        return backend.equals(o);
    }

    @Override
    public int hashCode() {
        return backend.hashCode();
    }

}
