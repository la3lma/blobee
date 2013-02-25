package no.rmz.testtools;

import static com.google.common.base.Preconditions.checkNotNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;

/**
 * Utility class for setting up conditions that are used to
 * ensure the required synchronization when setting up
 * tests that involves components running bits of pieces of
 * a test in different threads.
 */
public final class Conditions {

    private static  final Logger log =
            Logger.getLogger(Conditions.class.getName());

    /**
     * Utility class! No public constructor for you!
     */
    private Conditions() { }

    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public static void waitForCondition(
            final String description,
            final Lock lock,
            final Condition condition) {
        checkNotNull(description);
        checkNotNull(lock);
        checkNotNull(condition);

        try {
            lock.lock();
            log.log(Level.INFO, "Awaiting condition {0}", description);
            condition.await();
            log.log(Level.INFO,
                    "Just finished waiting for condition {0}", description);
        } catch (InterruptedException ex) {
            fail("Interrupted: " + ex);
        } finally {
            lock.unlock();
        }
    }

    public static void signalCondition(
            final String description,
            final Lock lock,
            final Condition condition) {
        try {
            lock.lock();
            log.log(Level.INFO, "Signalling condition {0}", description);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}
