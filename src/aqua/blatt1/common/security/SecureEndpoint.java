package aqua.blatt1.common.security;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint extends Endpoint {
    private final Endpoint endpoint;
    //private static final String CRYPTO_ALGORITM = "AES";
    //private static final byte[] KEY_MATERIAL = "CAFEBABECAFEBABE".getBytes();
    //private final SecretKey
    // Spec sks = new SecretKeySpec(KEY_MATERIAL, CRYPTO_ALGORITM);

    private Cipher decryptor;
    private Cipher encryptor;
    private static final String CRYPTO_ALGORITM = "RSA";
    private static final int key_size = 4096;
    KeyPairGenerator keyPairGen;
    Map<InetSocketAddress, Key> keys = new HashMap<>();

    private KeyPair keyPair;
    public SecureEndpoint() {
        this.endpoint = new Endpoint();
        createKeyPair();
    }
    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
        createKeyPair();
    }

    public void createKeyPair() {
        try {
            this.keyPairGen = KeyPairGenerator.getInstance(this.CRYPTO_ALGORITM);
            this.keyPairGen.initialize(this.key_size);
            this.keyPair  = this.keyPairGen.generateKeyPair();
            this.decryptor = Cipher.getInstance(this.CRYPTO_ALGORITM);
            this.decryptor.init(Cipher.DECRYPT_MODE, this.keyPair.getPrivate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(InetSocketAddress address, Serializable payload) {
        if(!this.keys.containsKey(address)) {
            this.endpoint.send(address, new KeyExchangeMessage(keyPair.getPublic()));
            Message msg = endpoint.blockingReceive();
            if (msg.getPayload() instanceof KeyExchangeMessage) {
                KeyExchangeMessageHandler(msg);
                encrypt(address,payload);
            }
        } else {
            encrypt(address, payload);
        }
    }

    private void encrypt(InetSocketAddress address, Serializable payload) {
        try {
            this.encryptor = Cipher.getInstance(this.CRYPTO_ALGORITM);
            this.encryptor.init(Cipher.ENCRYPT_MODE, this.keys.get(address));
            byte[] encrypted = encryptor.doFinal(convertToBytes(payload));
            endpoint.send(address, encrypted);
        } catch (Exception e) {
            e.printStackTrace();
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
        if (encryptedMessage.getPayload() instanceof KeyExchangeMessage) {
            if (!this.keys.containsKey(encryptedMessage.getSender())) {
                KeyExchangeMessageHandler(encryptedMessage);
            }
            this.endpoint.send(encryptedMessage.getSender(), new KeyExchangeMessage(this.keyPair.getPublic()));
            return new Message(null, null);
        }

        try {
            byte[] decrypted = decryptor.doFinal((byte[]) encryptedMessage.getPayload());
            return new Message((Serializable) convertFromBytes(decrypted), encryptedMessage.getSender());
        } catch (IllegalBlockSizeException | ClassNotFoundException | IOException | BadPaddingException e) {
            e.printStackTrace();
        }

        return new Message(null, null);
    }

    public void KeyExchangeMessageHandler(Message m){
        keys.put(m.getSender(),((KeyExchangeMessage) m.getPayload()).getKey());
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
