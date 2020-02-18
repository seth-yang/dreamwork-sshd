package org.dreamwork.network.sshd;

import org.apache.sshd.server.SshServer;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.IDatabase;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.sshd.cmd.PasswordCommand;
import org.dreamwork.network.sshd.cmd.ShutdownCommand;
import org.dreamwork.network.sshd.cmd.UserCommand;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.network.sshd.data.schema.UserSchema;
import org.dreamwork.persistence.DatabaseSchema;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.dreamwork.network.sshd.Keys.CFG_DB_FILE;

public class Sshd {
    private static final Logger logger = LoggerFactory.getLogger (Sshd.class);
    private MainShellCommand shell     = new MainShellCommand ();

    private IConfiguration conf;
    private IDatabase database;
    private SshServer server;

    public Sshd (IConfiguration conf) {
        this.conf = conf;
    }

    public void init (IDatabase database) throws NoSuchAlgorithmException, IOException {
        if (database != null) {
            this.database = database;
        } else {
            String dbFile = conf.getString (CFG_DB_FILE);
            if (dbFile == null) {
                dbFile = System.getProperty ("user.home") + "/.ssh-server/database.db";
            }
            this.database = createDatabase (dbFile);
        }
        shell.setDatabase (this.database);

        initDatabase ();
    }

    private IDatabase createDatabase (String file) throws IOException {
        File db  = new File (file);
        File dir = db.getParentFile ();
        if (logger.isTraceEnabled ()) {
            logger.trace ("trying to create/get the database from: {}", db.getCanonicalPath ());
        }
        if (!dir.exists () && !dir.mkdirs ()) {
            logger.warn ("can't create dir: {}, falling down to default location", dir.getCanonicalPath ());

            dir = new File (System.getProperty ("user.home"), ".ssh-server");
            if (!dir.exists () && !dir.mkdirs ()) {
                logger.error ("this will never happen, but it does, shutdown the application");
                throw new IOException ("init database fault");
            }
            db = new File (dir, "sshd-server.db");
        }
        return SQLite.get (db.getCanonicalPath ());
    }

    private void initDatabase () throws NoSuchAlgorithmException {
        if (logger.isTraceEnabled ()) {
            logger.trace ("register User Schema ...");
        }
        DatabaseSchema.register (UserSchema.class);
        if (!database.isTablePresent (User.class)) {
            UserSchema schema = new UserSchema ();
            if (logger.isTraceEnabled ()) {
                logger.trace ("table [{}] not exits, create a new one", schema.getTableName ());
            }
            database.execute (schema.getCreateDDL ());
            if (logger.isTraceEnabled ()) {
                logger.trace ("table created.");
            }

            byte[] buff = "123456".getBytes ();
            buff = AlgorithmUtil.md5 (buff);
            String password = StringUtil.byte2hex (buff, false);
            User user = new User ();
            user.setUserName ("root");
            user.setPassword (password);
            database.save (user);
        }
    }

    public void bind () throws Exception {
        if (logger.isTraceEnabled ()) {
            logger.trace ("starting the sshd server ...");
        }

        String caRoot = conf.getString (Keys.CFG_SSHD_CA_DIR);
        if (StringUtil.isEmpty (caRoot)) {
            caRoot = System.getProperty ("user.home") + "/.ssh-server/known-hosts";
        }

        int port = conf.getInt (Keys.CFG_SSHD_PORT, 50022);
        server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (port);
        server.setPasswordAuthenticator (new DatabaseAuthenticator (database));
        server.setKeyPairProvider (new FileSystemHostKeyProvider (caRoot));
        server.setShellFactory (channel -> shell);
        shell.registerCommands (
                new PasswordCommand (database),
                new UserCommand (database),
                new ShutdownCommand (this)
        );
        server.start ();

        if (logger.isInfoEnabled ()) {
            logger.info ("sshd server listen on {}:{}", server.getHost (), port);
        }
    }

    public void unbind () throws IOException {
        if (shell != null) {
            shell.notifyShutdown ();
        }
        if (server != null) {
            server.stop ();
        }
        System.exit (0);
    }

    public Sshd registerCommands (Command... commands) {
        shell.registerCommands (commands);
        return this;
    }
}