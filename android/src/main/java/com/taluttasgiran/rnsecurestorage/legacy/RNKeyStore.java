package com.taluttasgiran.rnsecurestorage.legacy;

import android.content.Context;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;

public class RNKeyStore {
    private byte[] decryptRsaCipherText(PrivateKey privateKey, byte[] cipherTextBytes) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance(Constants.RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return decryptCipherText(cipher, cipherTextBytes);
    }

    private byte[] decryptAesCipherText(SecretKey secretKey, byte[] cipherTextBytes) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance(Constants.AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return decryptCipherText(cipher, cipherTextBytes);
    }

    private byte[] decryptCipherText(Cipher cipher, byte[] cipherTextBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(cipherTextBytes);
        CipherInputStream cipherInputStream = new CipherInputStream(bais, cipher);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int bytesRead = cipherInputStream.read(buffer);
        while (bytesRead != -1) {
            baos.write(buffer, 0, bytesRead);
            bytesRead = cipherInputStream.read(buffer);
        }
        return baos.toByteArray();
    }

    private PrivateKey getPrivateKey(String alias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        keyStore.load(null);
        return (PrivateKey) keyStore.getKey(alias, null);
    }

    private SecretKey getSymmetricKey(Context context, String alias) throws GeneralSecurityException, IOException {
        byte[] cipherTextBytes = Storage.readValues(context, Constants.SKS_KEY_FILENAME + alias);
        return new SecretKeySpec(decryptRsaCipherText(getPrivateKey(alias), cipherTextBytes), Constants.AES_ALGORITHM);
    }

    public String getPlainText(Context context, String alias) throws GeneralSecurityException, IOException {
        SecretKey secretKey = getSymmetricKey(context, alias);
        byte[] cipherTextBytes = Storage.readValues(context, Constants.SKS_DATA_FILENAME + alias);
        return new String(decryptAesCipherText(secretKey, cipherTextBytes), "UTF-8");
    }

    public boolean exists(Context context, String alias) throws IOException {
        return Storage.exists(context, Constants.SKS_DATA_FILENAME + alias);
    }

    private KeyStore getKeyStore() throws KeyStoreException {
        try {
            return KeyStore.getInstance(Constants.KEYSTORE_PROVIDER_1);
        } catch (Exception err) {
            try {
                return KeyStore.getInstance(Constants.KEYSTORE_PROVIDER_2);
            } catch (Exception e) {
                return KeyStore.getInstance(Constants.KEYSTORE_PROVIDER_3);
            }
        }
    }

    public void removeOldKey(Context context, String key) {
        ArrayList<Boolean> fileDeleted = new ArrayList<>();
        try {
            for (String filename : new String[]{
                    Constants.SKS_DATA_FILENAME + key,
                    Constants.SKS_KEY_FILENAME + key,
            }) {
                fileDeleted.add(context.deleteFile(filename));
            }
            
            if (!fileDeleted.get(0) || !fileDeleted.get(1)) {
                Log.w(Constants.TAG, "Could not delete one or more legacy keystore files for key: " + key);
            }
        } catch (Exception e) {
            Log.w(Constants.TAG, "Error while trying to delete legacy keystore files: " + e.getMessage());
        }
    }
}