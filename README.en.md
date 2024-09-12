# dreamwork-sshd

### Introduction
Use the apache sshd framework to provide a usable sshd server-side semi-finished product. 
Rapidly develop ssh-based CLI applications.

## Software Architecture
- The underlying `apache sshd` framework is used to provide `SSH` network services.
- Built-in implementation of
  * RFC 318 (TELNET),
  * RFC 854 – RFC 861 (TELNET sub-options)
  * RFC 1073 (TELNET Window Size)

Java implementation of telnet protocol stack
- Extend `org.dreamwork.telnet.command.Command` to quickly develop commands to implement functions

## Instructions
Importing dependencies
```xml
<dependency>
    <groupId>io.github.seth-yang</groupId>
    <artifactId>dreamwork-sshd</artifactId>
    <version>1.0.0</version>
</dependency>
```
### The simplest example:
```java
package org.dreamwork.sshd.example;

import org.dreamwork.config.IConfiguration;
import org.dreamwork.config.PropertyConfiguration;
import org.dreamwork.network.sshd.Sshd;

import java.util.Properties;

public class BasicSshdApplication {
    public static void main (String[] args) throws Exception {
        IConfiguration conf = new PropertyConfiguration (new Properties ());
        
        Sshd sshd = new Sshd (conf);
        sshd.init (null);
        sshd.bind ();
    }
}
```
Then log in to the service through the command
```bash
ssh -p 50022 root@127.0.0.1
```
The default password of root is `123456`. You can use the `passwd` command to change the root password after 
logging in to the sshd service

### Configure sshd service
- The sshd service listens to the `50022` port by default
- The default storage location of the ca file is ${user.home}/.ssh-server/known-hosts
- The default storage location of the sqlite database file is ${user.home}/.ssh-server/database.db

You can modify these parameters through configuration
- The key `service.sshd.port` is used to modify the listening port
- The key `service.sshd.cert.file` is used to modify the storage path of the ca file
- The key `database.file` is used to modify the storage path of the sqlite database file
- The key `default.root.password` is used to modify the default root password

All the above key values can be overwritten by adding system properties

### Built-in database
`dreamwork sshd` uses a database to store user data and System Configuration Parameters

`org.dreamwork.network.sshd.Sshd` passes a database object through the `init (IDatabase)` method. 
When the method passes `null` as a parameter, a sqlite3 file database will be created internally. 
When you want sshd-server and your application to share a database, the parameter of the `init` method 
must be a **non-null** database object.

## Commands
Commands provide a set of human-machine interfaces with specific functions for `sshd-server`, 
and provide services by registering the implementation class of `org.dreamwork.telnet.command.Command` 
on `org.dreamwork.network.sshd.Sshd`.

### Built-in commands
In the above basic implementation, `sshd-server` has a set of built-in commands. 
You can view them through the command `help` after logging in to the service:
```text
console> h
clear       clear the screen and home cursor
echo        shows something
env         show environment
exit        alias for command 'quit'
help    h   show this help list
history     show history
passwd      change current or spec user's password
quit    q   exit
set         setting console env
shutdown    shutdown the sshd server
unset       unset a console env
user        user management
```

### Extend custom commands
Customize commands by extending the `org.dreamwork.telnet.command.Command` abstract class.

#### A simple custom command
Let's implement a simple command: `datetime` to display the current time
```java
package org.dreamwork.sshd.example;

import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class DateTimeCommand extends Command {
    private SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
    
    public DateTimeCommand () {
        super ("datetime", null, "show current date time");
    }

    /**
    * Execute command
    *
    * @param console current console
    * @throws IOException io exception
    */
    @Override
    public void perform (Console console) throws IOException {
        console.println (sdf.format (System.currentTimeMillis ()));
    }
}
```
- **Constructor**: prototype is `Command (String name, String alias, String description)`, where:
- **`name`**: specifies the name of the command, which is the main command entered in the console
- **`alias`**: alias of the command, which can provide a short alias for the command
- **`description`**: a short description of the command, which will be used by the built-in command `help`
- **Command processing function**: signature is `void perform (Console console) throws IOException`, where:
- **`console`**: the console object, which can print the command processing results

Now, we can register this command to `sshd-server`
```java
public class BasicSshdApplication {
    public static void main (String[] args) throws Exception {
        IConfiguration conf = new PropertyConfiguration (new Properties ());
        
        Sshd sshd = new Sshd (conf);
        sshd.init (null);
        
        // Register DateTimeCommand
        sshd.registerCommands (new DateTimeCommand ());
        
        sshd.bind ();
    }
}
```
Start the program again and use the ssh command to log in to the server, type the help command
```text
console> h
clear           clear the screen and home cursor
datetime        show current date time
echo            shows something
env             show environment
exit            alias for command 'quit'
help        h   show this help list
history         show history
passwd          change current or spec user's password
quit        q   exit
set             setting console env
shutdown        shutdown the sshd server
unset           unset a console env
user            user management
```
One more command
```text
datetime        show current date time
```
Execute command
```text
console> datetime
2020-02-25 19:37:41
console>
```

## In-depth `sshd-server` and `Command`
For more advanced/in-depth content, please refer to "[sshd-server User Guide](https://seth-yang.github.io/dreamwork-sshd/user-guide.html)"