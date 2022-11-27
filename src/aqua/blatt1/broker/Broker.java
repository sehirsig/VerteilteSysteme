package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.LinkedList;
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

            if (msg.getPayload() instanceof DeregisterRequest)
                deregister(msg);

            if (msg.getPayload() instanceof NameResolutionRequest)
                resoluteId(msg);

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

            int clientIndex = cc.indexOf(msg.getSender());
            if (cc.size() == 1) {
                InetSocketAddress client = cc.getClient(clientIndex);
                endpoint.send(client, new NeighborUpdate(client, client));
                endpoint.send(client, new Token());
            } else {
                InetSocketAddress client = cc.getClient(clientIndex);
                InetSocketAddress leftNeighbor = cc.getLeftNeighorOf(clientIndex);
                InetSocketAddress rightNeighbor = cc.getRightNeighorOf(clientIndex);
                endpoint.send(client, new NeighborUpdate(leftNeighbor, rightNeighbor));
                endpoint.send(leftNeighbor, new NeighborUpdate(null, client));
                endpoint.send(rightNeighbor, new NeighborUpdate(client, null));
            }

            Heimatsverzeichnis.add(new Heimatakte(clientName, msg.getSender()));
            endpoint.send(msg.getSender(), new RegisterResponse(clientName));
            System.out.println("New Client added: " + clientName);
            lock.writeLock().unlock();
        }

        public static void deregister(Message msg) {
            lock.writeLock().lock();
            DeregisterRequest msg_de = (DeregisterRequest) msg.getPayload();
            int getIndex = cc.indexOf(msg.getSender());

            if (cc.size() == 1) {
                InetSocketAddress client = cc.getClient(0);
                endpoint.send(client, new NeighborUpdate(client, client));
            } else {
                InetSocketAddress leftNeighbor = cc.getLeftNeighorOf(getIndex);
                InetSocketAddress rightNeighbor = cc.getRightNeighorOf(getIndex);
                endpoint.send(leftNeighbor, new NeighborUpdate(null, rightNeighbor));
                endpoint.send(rightNeighbor, new NeighborUpdate(leftNeighbor, null));
            }

            cc.remove(getIndex);
            System.out.println("Removed: " + msg_de.getId());
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
                receiverIndex = cc.getLeftNeighorOf(senderIndex);
                endpoint.send(receiverIndex, new HandoffRequest(fish));
            } else if (direction == Direction.RIGHT) {
                receiverIndex = cc.getRightNeighorOf(senderIndex);
                endpoint.send(receiverIndex, new HandoffRequest(fish));
            } else {
                System.out.println("Direction error");
            }
            lock.readLock().unlock();
        }

        public static void resoluteId(Message msg) {
            NameResolutionRequest msg_de = (NameResolutionRequest) msg.getPayload();

            String findTankId = msg_de.getTankId();

            for (var tuple : Heimatsverzeichnis) {
                if(tuple.tankId.equals(findTankId)) {
                    endpoint.send(msg.getSender(), new NameResolutionResponse(tuple.getHeimat(), msg_de.getRequestId()));
                }
            }
        }
    }
    static int NUMTHREADS = 16;

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
        //nicht executor nehmen, sondern neuen Thread
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

    public static class Heimatakte {
        private final String tankId;
        private final InetSocketAddress heimat;

        public Heimatakte(String tankId, InetSocketAddress heimat) {
            this.tankId = tankId;
            this.heimat = heimat;
        }

        public String getTankId() {
            return this.tankId;
        }

        public InetSocketAddress getHeimat() {
            return this.heimat;
        }
    }

    public static volatile LinkedList<Heimatakte> Heimatsverzeichnis = new LinkedList<Heimatakte>();
}
