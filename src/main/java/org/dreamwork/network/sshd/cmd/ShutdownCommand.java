package org.dreamwork.network.sshd.cmd;

import org.dreamwork.network.sshd.Sshd;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;

public class ShutdownCommand extends Command {
    private Sshd server;
    public ShutdownCommand (Sshd sshd) {
        super ("shutdown", null, "shutdown the sshd server");
        server = sshd;
    }

    @Override
    public void perform (Console console) throws IOException {
        User user = console.getAttribute ("user");
        if (user == null || !"root".equals (user.getUserName ())) {
            console.errorln ("only root user can shutdown the service.");
            return;
        }

        if (server != null) {
            console.println ();
            for (int i = 3; i > 0; i -- ) {
                console.eraseToBeginOfLine ();
                console.setForegroundColor (TerminalIO.YELLOW);
                console.print ("server will shutdown in " + i + " seconds");
                console.setForegroundColor (TerminalIO.COLORINIT);
            }
            console.close ();
            server.unbind ();
        }
    }
}
