package org.dreamwork.network.sshd.cert;

import org.dreamwork.secure.AlgorithmMapping;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by seth.yang on 2019/12/24
 */
public class KeyTool {
    private static KeyPairGenerator g;

    public static PrivateKey readPrivateKey (byte[] buff) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec (buff);
        KeyFactory factory = KeyFactory.getInstance ("RSA");
        return factory.generatePrivate (spec);
    }

    public static PublicKey readPublicKey (byte[] buff) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec (buff);
        KeyFactory factory = KeyFactory.getInstance ("RSA");
        return factory.generatePublic (spec);
    }

    public synchronized static KeyPair createKeyPair () {
        try {
            if (g == null) {
                g = KeyPairGenerator.getInstance ("RSA");
                g.initialize (2048);
            }

            return g.generateKeyPair ();
        } catch (Exception ex) {
            throw new RuntimeException (ex);
        }
    }

    public static SecretKey generateKey () throws NoSuchAlgorithmException {
        KeyGenerator g = KeyGenerator.getInstance (AlgorithmMapping.BlockEncryption.AES192_CBC.jceName);
        g.init (128);
        return g.generateKey ();
    }

    public static SecretKey loadKey (byte[] code) {
        return new SecretKeySpec (code, "AES");
    }
}