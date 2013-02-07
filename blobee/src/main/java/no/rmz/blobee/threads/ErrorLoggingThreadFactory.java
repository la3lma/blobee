/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package no.rmz.blobee.threads;

import static com.google.common.base.Preconditions.checkNotNull;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * When using  an executor service, it is by default
 * difficult to know where threads originate, since the names
 * are generic, and also when unchecked exceptions are thrown
 * by the threads the default behavior is to ignore the exception
 * completely.  This factory adds a name and an uncaught exception
 * handler that logs the exceptions to the log of your choice.
 */
public final class ErrorLoggingThreadFactory implements ThreadFactory {

    /**
     * The log to log to.
     */
    private final Logger log;
    /**
     * The name that all the threads generated by this
     * factory will be based on.
     */
    final String name;
    /**
     * An exception handler that logs exceptions as severe to the logger.
     */
    private final UncaughtExceptionHandler exceptionHandler;

    /**
     *
     * @param name The base name to use when creating threads.
     * @param log  The log to log exceptions to.
     */
    public ErrorLoggingThreadFactory(final String name, final Logger log) {
        this.name = checkNotNull(name);
        this.log  = checkNotNull(log);
        this.exceptionHandler = new UncaughtExceptionHandler() {
            public void uncaughtException(final Thread t, final Throwable e) {
                log.log(Level.SEVERE, "Uncaught exception in thrad " + t, e);
            }
        };
    }

    @Override
    public Thread newThread(final Runnable r) {
        checkNotNull(r);
        final Thread thread = new Thread(r, name);
        thread.setUncaughtExceptionHandler(exceptionHandler);
        return thread;
    }
}