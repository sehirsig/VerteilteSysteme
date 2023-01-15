package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt1.common.security.SecureEndpoint;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker implements AquaBroker{
    ClientCollection<AquaClient> cc = new ClientCollection<AquaClient>();
    int tankcounter = 0;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        AquaBroker stub = (AquaBroker) UnicastRemoteObject.exportObject(new Broker(), 0);
        registry.bind(Properties.BROKER_NAME, stub);
    }

    public void register(AquaClient client) throws RemoteException {
        lock.writeLock().lock();
        if (tankcounter == 0) {
            client.receiveToken();
        }
        tankcounter++;

        cc.add("tank" + (cc.size() + 1), client);
        AquaClient leftClient = cc.getLeftNeighborOf(client);
        AquaClient rightClient = cc.getRightNeighborOf(client);
        client.onRegistration("tank" + cc.size());
        client.receiveNeighbor(leftClient, rightClient);
        if (tankcounter == 1) {
            client.receiveNeighbor(client, client);
        }
        leftClient.receiveNeighbor(cc.getLeftNeighborOf(leftClient), client);
        rightClient.receiveNeighbor(client, cc.getRightNeighborOf(rightClient));
        lock.writeLock().unlock();
    }

    public void deregister(AquaClient client) throws RemoteException {
        lock.writeLock().lock();
        AquaClient leftClient = cc.getLeftNeighborOf(client);
        AquaClient rightClient = cc.getRightNeighborOf(client);
        leftClient.receiveNeighbor(cc.getLeftNeighborOf(leftClient), rightClient);
        rightClient.receiveNeighbor(leftClient, cc.getRightNeighborOf(rightClient));
        cc.remove(cc.indexOf(client));
        tankcounter--;
        lock.writeLock().unlock();
    }

    public void resoluteId(AquaClient client, FishModel fish) throws RemoteException {
        int index = cc.indexOf(fish.getTankId());
        AquaClient foundClient = cc.getClient(index);
        foundClient.locationUpdate(fish.getId(), client);
    }
}
