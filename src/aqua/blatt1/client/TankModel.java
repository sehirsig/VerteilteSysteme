package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.broker.AquaBroker;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

import javax.swing.*;


public class TankModel extends Observable implements Iterable<FishModel>, AquaClient {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected AquaClient leftNeighbor;
    protected AquaClient rightNeighbor;
    protected boolean token;
    protected Timer tokenTimer;
    protected final AquaBroker broker;
    private Map<String, AquaClient> fishMap = new HashMap<>();
    public AquaClient client;


    public TankModel(AquaBroker broker) throws RemoteException {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.broker = broker;

        this.client = (AquaClient) UnicastRemoteObject.exportObject(this, 0);
    }

    public void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    @Override
    public void receiveNeighbor(AquaClient l, AquaClient r) throws RemoteException {
        this.leftNeighbor = l;
        this.rightNeighbor = r;
    }


    public void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishies.add(fish);
            fishMap.put(fish.getId(), this);
        }
    }

    public void receiveFish(FishModel fish) throws RemoteException {
        fish.setToStart();
        fishies.add(fish);

        if (fishMap.containsKey(fish.getId())) {
            fishMap.put(fish.getId(), null);
        } else {
            broker.resoluteId(this, fish);
        }
    }

    public String getId() {
        return id;
    }

    public void updateNeighbors(AquaClient leftN, AquaClient rightN) {
        System.out.println("Neighbor updated");
        if (leftN != null) {
            leftNeighbor = leftN;
        }
        if (rightN != null) {
            rightNeighbor = rightN;
        }
    }

    public AquaClient getLeftNeighbor() {
        return this.leftNeighbor;
    }

    public AquaClient getRightNeighbor() {
        return this.rightNeighbor;
    }

    public int getFishCounter() {
        return this.fishCounter;
    }

    public Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private void updateFishies() throws RemoteException {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();
            if (leftNeighbor == null || rightNeighbor == null) {
                receiveFish(fish);
            } else if (fish.hitsEdge()) {
                if (this.hasToken()) {
                    System.out.println("Sending fish over:  " + fish.getId());
                    Direction direction = fish.getDirection();
                    if (direction.equals(Direction.LEFT)) {
                        this.leftNeighbor.receiveFish(fish);
                    } else {
                        this.rightNeighbor.receiveFish(fish);
                    }
                } else {
                    System.out.println("No token, reverse fish: " + fish.getId());
                    fish.reverse();
                }
            }
            if (fish.disappears())
                it.remove();
        }
    }

    private void update() throws RemoteException {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() throws RemoteException {
        broker.register(this.client);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public void finish() {
        try {
            broker.deregister(this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public class TokenTimerTask extends TimerTask {
        @Override
        public void run() {
            token = false;
            try {
                leftNeighbor.receiveToken();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            System.out.println("Give token to left neighbour");
        }
    }

    public void receiveToken() {
        System.out.println("Received Token");
        this.token = true;
        TimerTask tokenTask = new TokenTimerTask();
        tokenTimer = new Timer(true);
        tokenTimer.schedule(tokenTask, 2000);
    }


    public boolean hasToken() {
        return this.token;
    }

    public void locationUpdate(String fishId, AquaClient newLoc) {
        fishMap.put(fishId, newLoc);
    }

    public void locateFishGlobally(String fishId) throws RemoteException {
        if (fishMap.get(fishId) != null) {
            locateFishLocally(fishId);
        } else {
            fishMap.get(fishId).locateFishGlobally(fishId);
        }
    }

    public void locateFishLocally(String fishId) {
        for (var fish : fishies) {
            if (fish.getId().equals(fishId)) {
                fish.toggle();
                System.out.println("Fish here: " + fishId);
            }
        }
    }
}