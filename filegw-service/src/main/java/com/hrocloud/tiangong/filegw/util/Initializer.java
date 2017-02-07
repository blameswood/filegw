package com.hrocloud.tiangong.filegw.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sean
 * Date: 14-6-23
 * Time: 10:19
 * To change this template use File | Settings | File Templates.
 */
public class Initializer {

    private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

    public static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            logger.debug("Cryptography restrictions removal not needed");
            return;
        }
        try {
        /*
         * Do the following, but with reflection to bypass access checks:
         *
         * JceSecurity.isRestricted = false;
         * JceSecurity.defaultPolicy.perms.clear();
         * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
         */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

            logger.debug("Successfully removed cryptography restrictions");
        } catch (Exception e) {
            logger.warn("Failed to remove cryptography restrictions", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        // This simply matches the Oracle JRE, but not OpenJDK.
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

    public static void init(){
        removeCryptographyRestrictions();
        try {
            String algorithm="AES";
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(256);
            SecretKey sk = keyGen.generateKey();
            byte[] rawKey = sk.getEncoded();

            SecretKeySpec KEY = new SecretKeySpec(rawKey, algorithm);

            Cipher cipher = Cipher.getInstance(algorithm);

            cipher.init(Cipher.ENCRYPT_MODE, KEY);

        }

        catch (InvalidKeyException e) {
            logger.error("AES ENCRYPT ERROR,check jce policy first,System terminated!");
            System.exit(-1);
        }
        catch(Exception e){
            logger.error("UserServiceApplicationContextInitializer Error",e);
            System.exit(-1);
        }
    }
}
