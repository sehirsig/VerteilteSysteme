package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    private class BrokerTask implements Runnable {
        Message message;
        BrokerTask(Message msg) {
            message = msg;
        }
        @Override
        public void run() {
            this.verarbeiten(message);
        }

        public void verarbeiten(Message msg) {
            if (msg.getPayload() instanceof RegisterRequest)
                register(msg);

            if (msg.getPayload() instanceof HandoffRequest)
                handoffFish(msg);

            if (msg.getPayload() instanceof DeregisterRequest)
                deregister(msg);

            if (msg.getPayload() instanceof PoisonPill) {
                System.out.println("Broker stopped with Poison Pill.");
                executor.shutdown();
                System.exit(0);
            }
        }

        public static void register(Message msg) {
            lock.writeLock().lock();
            String clientName = "tank" + tankcounter++;
            cc.add(clientName, msg.getSender());
            endpoint.send(msg.getSender(), new RegisterResponse(clientName));
            System.out.println("New Client added: " + clientName);
            lock.writeLock().unlock();
        }

        public static void deregister(Message msg) {
            lock.writeLock().lock();
            DeregisterRequest msg_de = (DeregisterRequest) msg.getPayload();
            int getIndex = cc.indexOf(msg.getSender());
            System.out.println("Removed: " + msg_de.getId());
            cc.remove(getIndex);
            lock.writeLock().unlock();
        }

        public static void handoffFish(Message msg) {
            lock.readLock().lock();
            HandoffRequest msg_de = (HandoffRequest) msg.getPayload();
            int senderIndex = cc.indexOf(msg.getSender());
            FishModel fish = msg_de.getFish();

            Direction direction = msg_de.getFish().getDirection();
            InetSocketAddress receiverIndex;

            if (direction == Direction.LEFT) {
                receiverIndex = (InetSocketAddress) cc.getLeftNeighorOf(senderIndex);
                endpoint.send(receiverIndex, new HandoffRequest(fish));
            } else if (direction == Direction.RIGHT) {
                receiverIndex = (InetSocketAddress) cc.getRightNeighorOf(senderIndex);
                endpoint.send(receiverIndex, new HandoffRequest(fish));
            } else {
                System.out.println("Direction error");
            }
            lock.readLock().unlock();
        }
    }
    static int NUMTHREADS = 4;

    volatile ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);

    static volatile ReadWriteLock lock = new ReentrantReadWriteLock();

    static Endpoint endpoint = new Endpoint(4711);
    static volatile ClientCollection<InetSocketAddress> cc = new ClientCollection<InetSocketAddress>();
    static volatile int tankcounter = 0;

    static volatile boolean stopRequested = false;

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public void broker() {
        executor.execute(() -> {
            JOptionPane.showMessageDialog(null, "Press OK to stop server");
            System.out.println("Broker stopped with OK Message box.");
            stopRequested = true;
        });
        while (!stopRequested) {
            Message msg = endpoint.blockingReceive();
            BrokerTask brokerTask = new BrokerTask(msg);
            executor.execute(brokerTask);
        }
        executor.shutdown();
    }
}
