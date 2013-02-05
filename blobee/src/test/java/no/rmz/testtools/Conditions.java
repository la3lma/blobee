package no.rmz.testtools;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;

public final class Conditions {

    private final static Logger log = Logger.getLogger(Conditions.class.getName());

    public static void waitForCondition(final String description, final Lock lock, final Condition condition) {
        try {
            lock.lock();
            log.log(Level.INFO, "Awaiting condition {0}", description);
            condition.await();
            log.log(Level.INFO, "Just finished waiting for condition {0}", description);
        }
        catch (InterruptedException ex) {
            fail("Interrupted: " + ex);
        }
        finally {
            lock.unlock();
        }
    }

    public static void signalCondition(final String description, final Lock lock, final Condition condition) {
        try {
            lock.lock();
            log.log(Level.INFO, "Signalling condition {0}", description);
            condition.signal();
        }
        finally {
            lock.unlock();
        }
    }
}
