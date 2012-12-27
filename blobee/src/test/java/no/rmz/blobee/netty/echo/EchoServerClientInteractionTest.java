package no.rmz.blobee.netty.echo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public final class EchoServerClientInteractionTest {
    private final static int  PORT = 7171;
    private final static String HOST = "localhost";
    private final static int FIRST_MESSAGE_SIZE = 256;

    private EchoServer echoServer;
    private EchoClient echoClient;


    @Before
    public void setUp() {
        echoServer = new EchoServer(PORT);
        echoClient = new EchoClient(HOST, PORT, FIRST_MESSAGE_SIZE);
    }

    @Test
    public void hello() {
        // Uncomment to run forever, watch the blinkenlights  :-)
        // echoServer.run();
        // echoClient.run();
    }
}
