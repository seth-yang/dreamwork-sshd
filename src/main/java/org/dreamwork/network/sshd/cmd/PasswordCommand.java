package org.dreamwork.network.sshd.cmd;

import org.dreamwork.db.IDatabase;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

import static org.dreamwork.network.sshd.cmd.CommandUtil.*;

/**
 * Created by seth.yang on 2019/11/14
 */
public class PasswordCommand extends Command {
    private String userName;
    private IDatabase database;

    public PasswordCommand (IDatabase database) {
        super ("passwd", null, "change current or spec user's password");
        this.database = database;
    }

    @Override
    public void setContent (String content) {
        userName = content;
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
//            String current = console.getEnv ("USER");
            User current = console.getAttribute ("user");
            if (userName == null) {
                userName = current.getUserName ();
            } else if (!"root".equals (current.getUserName ()) && !current.getUserName ().equals (userName)) {
                console.errorln ("You are not authorized to execute this command.");
                return;
            }

//            User currentUser = database.getByPK (User.class, current);
            console.write ("Please input current password: ");
            String current_password = console.readInput (true);

            String md5 = StringUtil.dump (AlgorithmUtil.md5 (current_password.getBytes ())).toLowerCase ();
            if (!md5.equals (current.getPassword ())) {
                console.errorln ("current password mismatched.");
                return;
            }

            String password = readPassword ("Please input new password", console);
            if (StringUtil.isEmpty (password)) {
                return;
            }

            User user = current;
            if (!current.getUserName ().equals (userName)) {
                user = database.getByPK (User.class, userName);
            }

            if (user != null) {
                byte[] buff = password.getBytes ();
                buff = AlgorithmUtil.md5 (buff);
                md5 = StringUtil.byte2hex (buff, false);
                user.setPassword (md5);
                database.update (user);
                console.setForegroundColor (TerminalIO.GREEN);
                console.println ("password change success.");
                console.setForegroundColor (TerminalIO.COLORINIT);
            } else {
                console.errorln (userName + " not exists.");
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();
        } finally {
            userName = null;
        }
    }
}