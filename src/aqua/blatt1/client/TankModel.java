package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;


public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final ClientCommunicator.ClientForwarder forwarder;

    protected InetSocketAddress leftNeighbor;

    protected InetSocketAddress rightNeighbor;

    protected boolean token;

    protected Timer timer;

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
    }

    synchronized void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            if (!(this.aufzeichnunsmodus == MODUS.IDLE)) {
                this.lokaler_zustand += 1;
            }

            fishies.add(fish);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        if (!(this.aufzeichnunsmodus == MODUS.IDLE)) {
            this.lokaler_zustand += 1;
        }
        fish.setToStart();
        fishies.add(fish);
    }

    public String getId() {
        return id;
    }

    public synchronized void updateNeighbors(InetSocketAddress leftN, InetSocketAddress rightN) {
        System.out.println("Neighbor updated");
        if (leftN != null) {
            leftNeighbor = leftN;
        }
        if (rightN != null) {
            rightNeighbor = rightN;
        }
    }

    public InetSocketAddress getLeftNeighbor() {
        return leftNeighbor;
    }

    public InetSocketAddress getRightNeighbor() {
        return rightNeighbor;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge()) {
                if (this.hasToken()) {
                    if (!(this.aufzeichnunsmodus == MODUS.IDLE)) {
                        this.lokaler_zustand -= 1;
                    }
                    System.out.println("Sending fish over:  " + fish.getId());
                    Direction direction = fish.getDirection();
                    if (direction.equals(Direction.LEFT)) {
                        forwarder.handOff(leftNeighbor, fish);
                    } else {
                        forwarder.handOff(rightNeighbor, fish);
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

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        forwarder.deregister(id);
    }

    public class TokenTimerTask extends TimerTask {
        @Override
        public void run() {
            token = false;
            forwarder.handToken(getLeftNeighbor());
            System.out.println("Give token to left neighbour");
        }
    }

    public synchronized void receiveToken() {
        System.out.println("Received Token");
        this.token = true;
        TimerTask tokenTask = new TokenTimerTask();
        timer = new Timer(true);
        timer.schedule(tokenTask, 2000);
    }

    public synchronized boolean hasToken() {
        return this.token;
    }

    public enum MODUS {IDLE, LEFT, RIGHT, BOTH}

    private MODUS aufzeichnunsmodus = MODUS.IDLE;

    protected int lokaler_zustand = -1;


    public synchronized void initiateSnapshot() {
        this.globalSnapshotComplete = false;
        this.lokaler_zustand = this.fishies.size();
        this.aufzeichnunsmodus = MODUS.BOTH;
        this.forwarder.sendSnapshotMarker(leftNeighbor);
        this.forwarder.sendSnapshotMarker(rightNeighbor);
        this.isInitiator = true;

        this.forwarder.handSnapshotToken(this.leftNeighbor, this.lokaler_zustand);
        this.lokaler_zustand = -1;
        this.hasSnapshotToken = false;
    }

    public synchronized void receiveSnapshotMarker(InetSocketAddress sender) {
        // Zustand c als leere Liste
        if (sender.equals(leftNeighbor)) {
            if (this.aufzeichnunsmodus == MODUS.IDLE) {
                this.lokaler_zustand = this.fishies.size();
                this.forwarder.sendSnapshotMarker(leftNeighbor);
                this.forwarder.sendSnapshotMarker(rightNeighbor);
                this.aufzeichnunsmodus = MODUS.RIGHT;
                return;
            } else if (this.aufzeichnunsmodus == MODUS.BOTH) {
                this.aufzeichnunsmodus = MODUS.RIGHT;
            } else if (this.aufzeichnunsmodus == MODUS.LEFT) {
                this.aufzeichnunsmodus = MODUS.IDLE;
            }
            if (this.hasSnapshotToken) {
                if (this.lokaler_zustand != -1) {
                    this.globalSnapshotCount += this.lokaler_zustand;
                    this.forwarder.handSnapshotToken(this.leftNeighbor, this.globalSnapshotCount);
                    this.hasSnapshotToken = false;
                    this.lokaler_zustand = -1;
                }
            }
        } else if (sender.equals(rightNeighbor)) {
            if (this.aufzeichnunsmodus == MODUS.IDLE) {
                this.lokaler_zustand = this.fishies.size();
                this.forwarder.sendSnapshotMarker(leftNeighbor);
                this.forwarder.sendSnapshotMarker(rightNeighbor);
                this.aufzeichnunsmodus = MODUS.LEFT;
                return;
            } else if (this.aufzeichnunsmodus == MODUS.BOTH) {
                this.aufzeichnunsmodus = MODUS.LEFT;
            } else if (this.aufzeichnunsmodus == MODUS.RIGHT) {
                this.aufzeichnunsmodus = MODUS.IDLE;
            }
            if (this.hasSnapshotToken) {
                if (this.lokaler_zustand != -1) {
                    this.globalSnapshotCount += this.lokaler_zustand;
                    this.forwarder.handSnapshotToken(this.leftNeighbor, this.globalSnapshotCount);
                    this.hasSnapshotToken = false;
                    this.lokaler_zustand = -1;
                }
            }
        }
    }

    protected boolean hasSnapshotToken = false;
    protected int globalSnapshotCount = 0;
    protected boolean isInitiator = false;

    protected boolean globalSnapshotComplete = false;

    public synchronized void receiveSnapshotToken(int count) {
        this.globalSnapshotCount = count;
        this.hasSnapshotToken = true;

        if (this.isInitiator) {
            //Zeige Anzahl Fische globalSnapshotCount
            this.globalSnapshotComplete = true;
            this.hasSnapshotToken = false;
            this.isInitiator = false;
            this.lokaler_zustand = -1;
        } else {
            if (this.lokaler_zustand != -1) {
                this.globalSnapshotCount += this.lokaler_zustand;
                this.forwarder.handSnapshotToken(this.leftNeighbor, this.globalSnapshotCount);
                this.hasSnapshotToken = false;
                this.lokaler_zustand = -1;
            }
        }
    }

}