package no.rmz.maputils;


import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class InstrumentedMapRegistry {
    private static final Set<InstrumentedHashMap> maps = new HashSet<InstrumentedHashMap>();


    public static void register(final InstrumentedHashMap map) {
        checkNotNull(map);
        synchronized (maps) {
            maps.add(map);
        }
    }


    public final static void log(final InstrumentedHashMap map, long size) {
        final Runtime rt = Runtime.getRuntime();
        final long    usedMemory = rt.totalMemory() - rt.freeMemory();
        final String  name = map.getName();
        final String  classname = map.getCreator().getClass().getName();
        final long    time = System.currentTimeMillis();
        final PrintStream ps = getPrintStream(classname, name);
        synchronized(ps) {
            ps.format("%d  %d\n", time, size);
            ps.flush();
        }
        final PrintStream ps2 = getPrintStream(InstrumentedMapRegistry.class.getName(), "usedMemory");
        synchronized(ps) {
            ps2.format("%d  %d\n", time, usedMemory / 100000); // XXX magic scale factor
            ps2.flush();
        }
    }

    private final static Map<String, PrintStream> printstreams =
            new ConcurrentHashMap<String, PrintStream>();

    private static PrintStream getPrintStream(final String classname, final String name) {
        checkNotNull(name);
        checkNotNull(classname);
        synchronized (printstreams) {
            if (printstreams.containsKey(name)) {
                return printstreams.get(name);
            } else {
                final PrintStream ps = newPrintStream(name);
                printstreams.put(name, ps);
                return ps;
            }
        }
    }

    public static void close() {
        synchronized (printstreams) {
            for (final PrintStream ps : printstreams.values()) {
                synchronized (ps) {
                    ps.close();
                }
            }
        }
    }

    final static String logdir = "logs"; // XXX

    private static PrintStream newPrintStream(final String name) {
        try {
            final File file = new File(logdir  + "/" + name + ".csv");
            if (file.exists()) {
                file.delete();
            }
            final  PrintStream printStream = new PrintStream(file);
            return printStream;
        }
        catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
