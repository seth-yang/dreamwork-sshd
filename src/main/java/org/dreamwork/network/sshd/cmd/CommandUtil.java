package org.dreamwork.network.sshd.cmd;

import org.dreamwork.telnet.Console;
import org.dreamwork.util.StringUtil;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by seth.yang on 2019/11/14
 */
class CommandUtil {
    static String readPassword (Console console) throws IOException {
        return readPassword ("Please input password", console);
    }

    static String readPassword (String prompt, Console console) throws IOException {
        for (int i = 0; i < 3; i ++) {
            console.write (prompt + ": ");
            String p1 = console.readInput (true);
            console.write (prompt + " again: ");
            String p2 = console.readInput (true);

            if (!Objects.equals (p1, p2)) {
                console.errorln ("password not matched.");
                console.println ();
            } else {
                return p1;
            }
        }

        return null;
    }

    static Boolean option (String prompt, boolean defaultValue, Console console) throws IOException {
        String expression = defaultValue ? "[Y/n]: " : "[y/N]: ";
        for (int i = 0; i < 3; i ++) {
            console.write (prompt);
            console.write (" ");
            console.write (expression);

            String answer = console.readInput (false);
            if (StringUtil.isEmpty (answer)) {
                return defaultValue;
            } else if ("y".equalsIgnoreCase (answer) || "yes".equalsIgnoreCase (answer)) {
                return true;
            } else if ("n".equalsIgnoreCase (answer) || "no".equalsIgnoreCase (answer)) {
                return false;
            }
        }

        return null;
    }

    static String readLine (String prompt, String defaultValue, Console console) throws IOException {
        String expression;
        if (StringUtil.isEmpty (defaultValue)) {
            expression = "";
        } else {
            expression = "[" + defaultValue + "] ";
        }

        console.write (prompt);
        console.write (expression);

        String answer = console.readInput (false);
        if (StringUtil.isEmpty (answer)) {
            return defaultValue;
        } else {
            return answer;
        }
    }
}