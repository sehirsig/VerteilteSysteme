package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;

import java.net.InetSocketAddress;

public class Broker {
    static Endpoint endpoint;
    static ClientCollection<InetSocketAddress> cc;
    static int tankcounter;

    public static void main(String[] args) {
        int port = 4711;
        endpoint = new Endpoint(port);
        cc = new ClientCollection<InetSocketAddress>();
        tankcounter = 1;
        System.out.println("Broker created on Port: " + port);
        broker();
    }

    public static void broker() {
        while (true) {
            Message msg = endpoint.blockingReceive();

            if (msg.getPayload() instanceof RegisterRequest)
                register(msg);

            if (msg.getPayload() instanceof HandoffRequest)
                handoffFish(msg);

            if (msg.getPayload() instanceof DeregisterRequest)
                deregister(msg);
        }
    }

    public static void register(Message msg) {
        String clientName = "tank" + tankcounter++;
        cc.add(clientName, msg.getSender());
        endpoint.send(msg.getSender(), new RegisterResponse(clientName));
        System.out.println("New Client added: " + clientName);
    }

    public static void deregister(Message msg) {
        DeregisterRequest msg_de = (DeregisterRequest) msg.getPayload();
        int getIndex = cc.indexOf(msg.getSender());
        System.out.println("Removed: " + msg_de.getId());
        cc.remove(getIndex);
    }

    public static void handoffFish(Message msg) {
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
    }
}
