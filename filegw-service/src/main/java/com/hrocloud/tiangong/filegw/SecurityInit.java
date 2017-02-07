package com.hrocloud.tiangong.filegw;

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
 * Created by hanzhihua on 2016/11/17.
 */
public class SecurityInit {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityInit.class);
    public static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            LOG.debug("Cryptography restrictions removal not needed");
            return;
        }
        try {
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

            LOG.debug("Successfully removed cryptography restrictions");
        } catch (Exception e) {
            LOG.warn("Failed to remove cryptography restrictions", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        // This simply matches the Oracle JRE, but not OpenJDK.
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

    public void init(){
        removeCryptographyRestrictions();
        try {
            String algorithm="AES";
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(256);
            SecretKey sk = keyGen.generateKey();
            byte[] rawKey = sk.getEncoded();

            SecretKeySpec key = new SecretKeySpec(rawKey, algorithm);

            Cipher cipher = Cipher.getInstance(algorithm);

            cipher.init(Cipher.ENCRYPT_MODE, key);

        }catch (InvalidKeyException e) {
            LOG.error("AES ENCRYPT ERROR,check jce policy first,System terminated!");
//            try {
//                Class.forName("java.lang.System").getMethod("exit", Integer.TYPE).invoke(null, 3);
//            }catch(Exception ex){
//
//            }

        }catch(Exception e){
            LOG.error("UserServiceApplicationContextInitializer Error", e);
//            try {
//                Class.forName("java.lang.System").getMethod("exit", Integer.TYPE).invoke(null, 3);
//            }catch(Exception ex){
//
//            }
        }
    }
}
