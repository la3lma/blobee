package no.rmz.maputils;


import static com.google.common.base.Preconditions.checkNotNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public final class InstrumentedMapRegistry {

    private  static final Logger log =
            Logger.getLogger(InstrumentedMapRegistry.class.getName());

    private static final Set<InstrumentedHashMap> MAPS =
            new HashSet<>();

    private  InstrumentedMapRegistry() {
    }



    public static void register(final InstrumentedHashMap map) {
        checkNotNull(map);
        synchronized (MAPS) {
            MAPS.add(map);
        }
    }


    public static  void log(
            final InstrumentedHashMap map,
            final long size) {
        final Runtime rt = Runtime.getRuntime();
        final long    usedMemory = rt.totalMemory() - rt.freeMemory();
        final String  name = map.getName();
        final String  classname = map.getCreator().getClass().getName();
        final long    time = System.currentTimeMillis();
        final PrintStream ps = getPrintStream(classname, name);
        synchronized (ps) {
            ps.format("%d  %d\n", time, size);
            ps.flush();
        }
        final PrintStream ps2 = getPrintStream(
                InstrumentedMapRegistry.class.getName(), "usedMemory");
        synchronized (ps) {
            ps2.format("%d  %d\n", time, usedMemory / 100000);
            ps2.flush();
        }
    }

    private static  final Map<String, PrintStream> PRINTSTREAMS =
            new ConcurrentHashMap<>();

    @SuppressWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    private static PrintStream getPrintStream(
            final String classname,
            final String name) {
        checkNotNull(name);
        checkNotNull(classname);
        synchronized (PRINTSTREAMS) {
            if (PRINTSTREAMS.containsKey(name)) {
                return PRINTSTREAMS.get(name);
            } else {
                final PrintStream ps = newPrintStream(name);
                PRINTSTREAMS.put(name, ps);
                return ps;
            }
        }
    }

    @SuppressWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    public static void close() {
        synchronized (PRINTSTREAMS) {
            for (final PrintStream ps : PRINTSTREAMS.values()) {
                synchronized (ps) {
                    ps.close();
                }
            }
        }
    }

    private static final String logdir = "logs"; // XXX

    private static PrintStream newPrintStream(final String name) {
        try {
            final File file = new File(logdir  + "/" + name + ".csv");
            if (file.exists()) {
                boolean delete = file.delete();
                if (!delete) {
                    log.info("Did not manage to delete file " + file);
                }
            }
            final  PrintStream printStream = new PrintStream(file);
            return printStream;
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
