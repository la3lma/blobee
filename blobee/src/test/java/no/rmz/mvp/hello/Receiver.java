package no.rmz.mvp.hello;

/**
 * We need an interface to receive something into a mock
 */
public interface Receiver<T> {
    public void receive(final T param);
}
