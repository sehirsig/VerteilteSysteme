package aqua.blatt1.common.security;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class SecureEndpoint extends Endpoint {
    private final Endpoint endpoint;
    private static final String CRYPTO_ALGORITM = "AES";
    private static final byte[] KEY_MATERIAL = "CAFEBABECAFEBABE".getBytes();
    private final SecretKeySpec sks = new SecretKeySpec(KEY_MATERIAL, CRYPTO_ALGORITM);
    private Cipher decryptor;
    private Cipher encryptor;

    public SecureEndpoint() {
        this.endpoint = new Endpoint();
        try {
            encryptor = Cipher.getInstance(CRYPTO_ALGORITM);
            decryptor = Cipher.getInstance(CRYPTO_ALGORITM);
            encryptor.init(Cipher.ENCRYPT_MODE, sks);
            decryptor.init(Cipher.DECRYPT_MODE, sks);
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint();
        try {
            encryptor = Cipher.getInstance(CRYPTO_ALGORITM);
            decryptor = Cipher.getInstance(CRYPTO_ALGORITM);
            encryptor.init(Cipher.ENCRYPT_MODE, sks);
            decryptor.init(Cipher.DECRYPT_MODE, sks);
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(InetSocketAddress address, Serializable payload) {
        encrypt(address, payload);
    }

    private void encrypt(InetSocketAddress address, Serializable payload) {
        try {
            System.out.println("Encrypt: " + payload.toString());
            encryptor.init(Cipher.DECRYPT_MODE, sks);
            byte[] encrypted = encryptor.doFinal(convertToBytes(payload));
            System.out.println("Encrypt: " + encrypted.toString());
            endpoint.send(address, encrypted);
        }catch (InvalidKeyException | IllegalBlockSizeException | IOException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }

    private Message decrypt(Message encryptedMessage) {
        try {
            byte[] decrypted = decryptor.doFinal(convertToBytes(encryptedMessage.getPayload()) );
            System.out.println("Decrypt: " + convertFromBytes(decrypted).toString());
            return new Message((Serializable) convertFromBytes(decrypted), encryptedMessage.getSender());
        } catch (IllegalBlockSizeException | ClassNotFoundException | IOException | BadPaddingException e) {
            e.printStackTrace();
        }

        return new Message(null, null);
    }

    @Override
    public Message blockingReceive() {
        Message encryptedMessage = endpoint.blockingReceive();
        return decrypt(encryptedMessage);
    }

    @Override
    public Message nonBlockingReceive() {

        Message encryptedMessage = endpoint.nonBlockingReceive();
        return decrypt(encryptedMessage);
    }
}
