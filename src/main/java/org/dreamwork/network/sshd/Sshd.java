package org.dreamwork.network.sshd;

import org.apache.sshd.server.SshServer;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.IDatabase;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.sshd.cmd.PasswordCommand;
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
    private static final Logger logger          = LoggerFactory.getLogger (Sshd.class);
    private static final MainShellCommand shell = new MainShellCommand ();

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
            this.database = createDatabase (conf.getString (CFG_DB_FILE));
        }

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

            dir = new File (System.getProperty ("user.home"), ".dreamwork-sshd");
            if (!dir.exists () && !dir.mkdirs ()) {
                logger.error ("this will never happen, but it does, shutdown the application");
                throw new IOException ("init database fault");
            }
            db = new File (dir, file);
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

        int port = conf.getInt (Keys.CFG_SSHD_PORT, 50022);
        server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (port);
        server.setPasswordAuthenticator (new DatabaseAuthenticator (database));
        server.setKeyPairProvider (new FileSystemHostKeyProvider (conf.getString (Keys.CFG_SSHD_CA_DIR)));
        server.setShellFactory (channel -> shell);
        shell.registerCommands (
                new PasswordCommand (database),
                new UserCommand (database)
        );
        server.start ();

        if (logger.isTraceEnabled ()) {
            logger.trace ("sshd server listen on {}:{}", server.getHost (), port);
        }
    }

    public void unbind () throws IOException {
        if (server != null) {
            server.stop ();
        }
    }

    public Sshd registerCommands (Command... commands) {
        shell.registerCommands (commands);
        return this;
    }

/*
    public SQLite initDatabase () throws NoSuchAlgorithmException, SQLException, IOException {
        if (logger.isTraceEnabled ()) {
            logger.trace ("initialing the database ...");
        }
        initDatabase (conf.getString (Keys.CFG_DB_FILE));
        if (logger.isTraceEnabled ()) {
            logger.trace ("the database initialed.");
        }

        return Context.db;
    }



    public SQLite getDatabase () {
        return Context.db;
    }



    public void initRootUser (IDatabase db) throws NoSuchAlgorithmException {
        DatabaseSchema.register (UserSchema.class);
        if (!db.isTablePresent (User.class)) {
            // create the user table
            db.execute (UserSchema.MAP.get (UserSchema.class).getCreateDDL ());

            User root = db.getByPK (User.class, "root");
            if (root == null) {
                // the root user not presented, create a new one
                byte[] buff = AlgorithmUtil.md5 ("123456".getBytes ());
                String password = StringUtil.byte2hex (buff, false);
                root = new User ();
                root.setUserName ("root");
                root.setPassword (password);
                db.save (root);
            }
        }
    }

    private void initDatabase (String file) throws SQLException, NoSuchAlgorithmException, IOException {
        File db  = new File (file);
        File dir = db.getParentFile ();
        if (logger.isTraceEnabled ()) {
            logger.trace ("trying to create/get the database from: {}", db.getCanonicalPath ());
        }
        if (!dir.exists () && !dir.mkdirs ()) {
            logger.error ("can't create dir: {}", dir.getCanonicalPath ());
            System.exit (-1);
            return;
        }
        SQLite sqlite = SQLite.get (file);
        if (logger.isDebugEnabled ()) {
            sqlite.setDebug (true);
        }
        Schema.registerAllSchemas ();
        if (!sqlite.isTablePresent ("t_device")) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("creating tables ...");
            }
            sqlite.createSchemas ();

            if (logger.isTraceEnabled ()) {
                logger.trace ("creating root user ...");
            }
            User user = new User ();
            user.setUserName ("root");
            user.setPassword (StringUtil.dump (AlgorithmUtil.md5 ("123456".getBytes ())).toLowerCase ());
            sqlite.save (user);

            initRootCA (sqlite);
        }
        Context.db = sqlite;
    }

    private void initRootCA (SQLite sqlite) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("creating root CA");
        }
        KeyPair pair = KeyTool.createKeyPair ();
        PrivateKey pri = pair.getPrivate ();
        PublicKey pub = pair.getPublic ();

        SystemConfig sc_private_key = new SystemConfig ();
        sc_private_key.setId (Keys.SYS_CONFIG.CFG_PRIMARY_KEY);
        sc_private_key.setValue (new String (Base64.encode (pri.getEncoded ())));
        sc_private_key.setEditable (false);

        SystemConfig sc_public_key  = new SystemConfig ();
        sc_public_key.setId (Keys.SYS_CONFIG.CFG_PUBLIC_KEY);
        sc_public_key.setValue (new String (Base64.encode (pub.getEncoded ())));
        sc_public_key.setEditable (false);

        sqlite.save (Arrays.asList (sc_private_key, sc_public_key));
    }
*/
}
