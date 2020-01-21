package org.dreamwork.network.sshd;

import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.dreamwork.concurrent.Looper;
import org.dreamwork.misc.Base64;
import org.dreamwork.network.sshd.cert.KeyTool;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

/**
 * Created by seth.yang on 2019/11/1
 */
public class FileSystemHostKeyProvider extends SimpleGeneratorHostKeyProvider {
    private static Path path;

    private final static Map<String, KeyPair> pairs = new TreeMap<> ();
    private final static List<KeyPair> cached_pairs = Collections.synchronizedList (new ArrayList<> ());
    private final static Logger logger = LoggerFactory.getLogger (FileSystemHostKeyProvider.class);
    private final static String LOOPER_NAME = "KEY_WRITER";

    static {
        Looper.create (LOOPER_NAME, 16);
    }

    private synchronized static void init () {
        if (Files.exists (path, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream in = Files.newInputStream (path, StandardOpenOption.READ)) {
                BufferedReader reader = new BufferedReader (new InputStreamReader (in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine ()) != null) {
                    line = line.trim ();
                    if (StringUtil.isEmpty (line)) {
                        continue;
                    }
                    String[] parts = line.split (" ");
                    String key = parts[0];
                    String alg = parts[1];
                    String tmp = parts[2];

                    if (logger.isTraceEnabled ()) {
                        logger.trace ("key -> {}", key);
                        logger.trace ("alg -> {}", alg);
                    }

                    byte[] buff = Base64.decode (tmp);
                    DataInputStream dis = new DataInputStream (new ByteArrayInputStream (buff));
                    int length = dis.readUnsignedShort ();
                    byte[] pri = new byte[length];
                    int read = dis.read (pri);
                    if (read != length) {
                        throw new RuntimeException ("expect " + length + " bytes, but read " + read + " bytes.");
                    }

                    length = dis.readUnsignedShort ();
                    byte[] pub = new byte[length];
                    read = dis.read (pub);
                    if (read != length) {
                        throw new RuntimeException ("expect " + length + " bytes, but read " + read + " bytes");
                    }

                    PrivateKey priKey = KeyTool.readPrivateKey (pri);
                    PublicKey pubKey = KeyTool.readPublicKey (pub);
                    pairs.put (key, new KeyPair (pubKey, priKey));
                }

                cached_pairs.addAll (pairs.values ());
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                logger.warn (ex.getMessage (), ex);
                throw new RuntimeException (ex);
            }
        }
    }

    public FileSystemHostKeyProvider (String dir) {
        synchronized (LOOPER_NAME) {
            if (path == null) {
                path = Paths.get (dir);

                init ();
            }
        }
    }

    private synchronized static void write () {
        if (!pairs.isEmpty ()) {
            if (!Files.exists (path, LinkOption.NOFOLLOW_LINKS)) {
                Path parent = path.getParent ();
                try {
                    Files.createDirectories (parent);
                } catch (IOException ex) {
                    logger.warn (ex.getMessage (), ex);
                    throw new RuntimeException (ex);
                }
            }

            try (OutputStream out = Files.newOutputStream (path, CREATE, TRUNCATE_EXISTING, WRITE)) {
                PrintStream printer = new PrintStream (out, true, "utf-8");
                int index = 0;
                for (String key : pairs.keySet ()) {
                    if (index > 0) {
                        printer.println ();
                    }
                    printer.print (key);
                    printer.print (' ');

                    KeyPair pair = pairs.get (key);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
                    DataOutputStream dos = new DataOutputStream (baos);
                    PrivateKey priKey = pair.getPrivate ();
                    PublicKey  pubKey = pair.getPublic ();

                    printer.print (priKey.getAlgorithm ());
                    printer.print (' ');

                    byte[] tmp = priKey.getEncoded ();
                    dos.writeShort (tmp.length);
                    dos.write (tmp);

                    tmp = pubKey.getEncoded ();
                    dos.writeShort (tmp.length);
                    dos.write (tmp);
                    dos.flush ();

                    tmp = baos.toByteArray ();
                    tmp = Base64.encode (tmp);
                    printer.write (tmp);

                    index ++;
                }

                out.flush ();
            } catch (IOException ex) {
                logger.warn (ex.getMessage (), ex);
            }
        }
    }

    @Override
    public synchronized List<KeyPair> loadKeys (SessionContext session) {
        InetAddress address = ((InetSocketAddress) session.getRemoteAddress ()).getAddress ();
        String name;
        if (address.isLoopbackAddress ()) {
            name = "127.0.0.1";
        } else {
            name = address.getHostAddress ();
        }

        if (!pairs.containsKey (name)) {
            pairs.computeIfAbsent (name, key -> KeyTool.createKeyPair ());
            Looper.runInLoop (LOOPER_NAME, FileSystemHostKeyProvider::write);
            cached_pairs.clear ();
            cached_pairs.addAll (pairs.values ());
        }

        return cached_pairs;
    }
}