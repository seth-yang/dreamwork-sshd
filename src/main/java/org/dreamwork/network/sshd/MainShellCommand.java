package org.dreamwork.network.sshd;

import org.apache.sshd.common.channel.ChannelPipedInputStream;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.dreamwork.concurrent.Looper;
import org.dreamwork.db.IDatabase;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.telnet.ConnectionData;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.SimpleCommandParser;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.CommandParser;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by seth.yang on 2019/10/29
 */
public class MainShellCommand implements Command {
    private static final class IoWrapper {
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
    }

    private IoWrapper currentWrapper = null;
    private IDatabase database;

    private CommandParser parser = new SimpleCommandParser (true);

    private final Logger logger = LoggerFactory.getLogger (MainShellCommand.class);
    private final Set<ChannelSession> cachedChannels = new HashSet<> ();

    public MainShellCommand () {

    }

    public MainShellCommand (IDatabase database) {
        this ();
        this.database = database;
    }

    public void setDatabase (IDatabase database) {
        this.database = database;
    }

    @Override
    public void setInputStream (InputStream in) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting input stream into : {}", in);
        }

        synchronized (IoWrapper.class) {
            if (currentWrapper == null) {
                currentWrapper = new IoWrapper ();
            }
            currentWrapper.in = in;
        }
    }

    @Override
    public void setOutputStream (OutputStream out) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting output stream into: {}", out);
        }
        synchronized (IoWrapper.class) {
            if (currentWrapper == null) {
                currentWrapper = new IoWrapper ();
            }
            currentWrapper.out = out;
        }
    }

    @Override
    public void setErrorStream (OutputStream err) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting err stream into : {}", err);
        }
        synchronized (IoWrapper.class) {
            if (currentWrapper == null) {
                currentWrapper = new IoWrapper ();
            }
            currentWrapper.err = err;
        }
    }

    /**
     * Set the callback that the shell has to call when it is closed.
     *
     * @param callback The {@link ExitCallback} to call when shell is closed
     */
    @Override
    public void setExitCallback (ExitCallback callback) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("exit callback set: {}", callback);
        }
    }

    private void onWindowChanged (Map<String, String> map, ConnectionData cd) {
        String term     = map.get (Environment.ENV_TERM);
        String s_column = map.get (Environment.ENV_COLUMNS);
        String s_row    = map.get (Environment.ENV_LINES);

        if (logger.isTraceEnabled ()) {
            logger.trace ("width = {}, height = {}, term = {}", s_column, s_row, term);
        }

        if (!StringUtil.isEmpty (term)) {
            cd.setNegotiatedTerminalType (term);
        }
        if (!StringUtil.isEmpty (s_column) && !StringUtil.isEmpty (s_row)) {
            int column = Integer.parseInt (s_column);
            int row    = Integer.parseInt (s_row);
            cd.setTerminalGeometry (column, row);
        }
    }

    @Override
    public void start (ChannelSession channel, Environment env) {
        if (logger.isDebugEnabled ()) {
            logger.debug ("opening channel[{}]...", channel.getSession ().getRemoteAddress ());
        }

        synchronized (IoWrapper.class) {
            if (currentWrapper != null) {
                InputStream in = currentWrapper.in;
                OutputStream out = currentWrapper.out;
                OutputStream err = currentWrapper.err;
                currentWrapper = null;

                channel.getProperties ().put (Keys.KEY_IN, in);
                channel.getProperties ().put (Keys.KEY_OUT, out);
                channel.getProperties ().put (Keys.KEY_ERR, err);

                ConnectionData cd = new ConnectionData ();
                {
                    Map<String, String> map = env.getEnv ();
                    onWindowChanged (map, cd);
                    env.addSignalListener ((channel1, signal) -> {
                        if (signal == Signal.WINCH) {
                            onWindowChanged (map, cd);
                        }
                    });
                }
                Console console = new Console (in, out, cd, true);
                console.setCommandParser (parser);
                for (Map.Entry<String, String> e : env.getEnv ().entrySet ()) {
                    String key = e.getKey ();
                    console.setEnv (key, e.getValue ());
                    if ("USER".equals (key)) {
                        User user = database.getByPK (User.class, e.getValue ());
                        if (user != null) {
                            console.setAttribute ("user", user);
                        }
                    }
                }
                channel.getProperties ().put ("connect.data", cd);
                channel.getProperties ().put ("console", console);
                Looper.invokeLater (() -> {
                    try {
                        console.loop ();
                        console.close ();
                        channel.getSession ().close ();
                        channel.close ();
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }
                });
                cachedChannels.add (channel);
            }
        }
    }

    @Override
    public void destroy (ChannelSession channel) {
        if (channel.isOpen ()) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("MainShellCommand destroy, channel = {}", channel.getSession ().getRemoteAddress ());
            }
            Console console = (Console) channel.getProperties ().get ("console");
            if (console != null) {
                try {
                    int columns = "server will shutdown in 0 seconds".length ();
                    ChannelPipedInputStream in = (ChannelPipedInputStream) channel.getProperties ().get (Keys.KEY_IN);

                    byte[] buff = new byte[1];
                    in.receive (buff, 0, 1);
                    console.println ();
                    for (int i = 3; i > 0; i--) {
                        in.receive (buff, 0, 1);
                        console.moveLeft (columns);

                        in.receive (buff, 0, 1);
                        console.setForegroundColor (TerminalIO.YELLOW);

                        in.receive (buff, 0, 1);
                        console.print ("server will shutdown in " + i + " seconds");

                        try {
                            Thread.sleep (1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace ();
                        }
                    }
                    in.receive (buff, 0, 1);
                    console.println ();
                    console.flush ();
                } catch (IOException ex) {
                    logger.warn (ex.getMessage (), ex);
                }
            }
            channel.close (true);
            channel.getProperties ().clear ();
            cachedChannels.remove (channel);
        } else {
            logger.warn ("the channel[{}] is closed.", channel.getSession ().getRemoteAddress ());
        }
    }

    void registerCommands (org.dreamwork.telnet.command.Command... commands) {
        parser.registerCommand (commands);
    }

    void notifyShutdown () {
        if (logger.isTraceEnabled ()) {
            logger.trace ("there're {} channels in cache", cachedChannels.size ());
            for (ChannelSession channel : cachedChannels) {
                logger.trace ("  * {}", channel.getSession ().getRemoteAddress ());
            }
        }
        CountDownLatch latch = new CountDownLatch (cachedChannels.size ());
        for (ChannelSession channel : cachedChannels) {
            Looper.invokeLater (() -> {
                if (logger.isTraceEnabled ()) {
                    logger.trace ("closing channel: {}", channel);
                }
                destroy (channel);
                if (logger.isDebugEnabled ()) {
                    logger.debug ("channel [{}] destroyed.", channel.getSession ().getRemoteAddress ());
                }
                latch.countDown ();
            });
        }
        try {
            latch.await ();
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }
        if (logger.isTraceEnabled ()) {
            logger.trace ("all channels destroyed.");
        }
    }
}