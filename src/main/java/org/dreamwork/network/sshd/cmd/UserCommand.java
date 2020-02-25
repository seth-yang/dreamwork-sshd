package org.dreamwork.network.sshd.cmd;

import org.dreamwork.db.IDatabase;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

/**
 * Created by seth.yang on 2019/11/14
 */
public class UserCommand extends Command {
    private String action, userName, message;
    private IDatabase db;

    public UserCommand (IDatabase db) {
        super ("user", null, "user management");
        this.db = db;
    }

    @Override
    public void parse (String... options) {
        if (options.length == 1 && ("--help".equals (options [0]) || "-h".equals (options [0]))) {
            action = "help";
        } else if (options.length != 2) {
            message = "invalid arguments";
        } else {
            action = options[0];
            userName = options[1];
        }
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
            User current = console.getAttribute ("user");
            if (!"root".equals (current.getUserName ())) {
                console.errorln ("You are not authorized to execute this command.");
                return;
            }

            if (!StringUtil.isEmpty (message)) {
                console.errorln (message);
                return;
            }

            if ("help".equals (action)) {
                showHelp (console);
                return;
            }

            if (StringUtil.isEmpty (action) || StringUtil.isEmpty (userName)) {
                console.errorln ("invalid arguments");
                return;
            }

            User user = db.getByPK (User.class, userName);
            switch (action) {
                case "add":
                    if (user != null) {
                        console.errorln ("user " + userName + " already exists.");
                        return;
                    }

                    String password = console.readPassword ();
                    if (StringUtil.isEmpty (password)) {
                        return;
                    }

                    user = new User ();
                    user.setUserName (userName);
                    user.setPassword (StringUtil.dump (AlgorithmUtil.md5 (password.getBytes ())).toLowerCase ());
                    db.save (user);

                    console.setForegroundColor (TerminalIO.GREEN);
                    console.println ("user add success.");
                    console.setForegroundColor (TerminalIO.COLORINIT);
                    break;
                case "del":
                case "delete":
                    if ("root".equals (userName)) {
                        console.errorln ("you CAN NOT delete user root.");
                        break;
                    }
                    if (user != null) {
                        Boolean answer = console.option ("Are you sure to delete the user", false);
                        if (answer != null && answer) {
                            db.delete (user);
                            console.setForegroundColor (TerminalIO.GREEN);
                            console.println ("user delete success.");
                            console.setForegroundColor (TerminalIO.COLORINIT);
                        }
                    } else {
                        console.errorln ("user " + userName + " not found.");
                    }
                    break;
                default:
                    console.errorln ("invalid command: " + action);
                    break;
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();
        } finally {
            action   = null;
            userName = null;
            message  = null;
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("user add|del[ete] <username>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("user -h | --help    show this list");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }
}