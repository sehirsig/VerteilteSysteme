package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt1.common.security.SecureEndpoint;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new SecureEndpoint();
	}

	public class ClientForwarder {
		// Nicht an Broker, sondern an nachbar
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(InetSocketAddress receiver, FishModel fish) {
			Direction direction = fish.getDirection();
			if (direction.equals(Direction.LEFT)) {
				endpoint.send(receiver, new HandoffRequest(fish));
			} else if (direction.equals(Direction.RIGHT)) {
				endpoint.send(receiver, new HandoffRequest(fish));
			}
		}

		public void handToken(InetSocketAddress receiver) {
			endpoint.send(receiver, new Token());
		}

		public void sendSnapshotMarker(InetSocketAddress receiver) {
			endpoint.send(receiver, new SnapshotMarker());
		}
		public void handSnapshotToken(InetSocketAddress receiver, int count) {
			endpoint.send(receiver, new SnapshotToken(count));
		}

		public void searchFish(InetSocketAddress receiver, String fishId) {
			endpoint.send(receiver, new LocationRequest(fishId));
		}

		public void sendNameResolutionRequest(String tankId, String requestId) {
			endpoint.send(broker, new NameResolutionRequest(tankId, requestId));
		}

		public void sendLocationUpdate(InetSocketAddress receiver, String requestId) {
			endpoint.send(receiver, new LocationUpdate(requestId));

		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLeaseDuration());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighborUpdate) {
					tankModel.updateNeighbors(((NeighborUpdate) msg.getPayload()).getLeftNeighbor(), ((NeighborUpdate) msg.getPayload()).getRightNeighboar());
				}

				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveSnapshotMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof SnapshotToken) {
					tankModel.receiveSnapshotToken(((SnapshotToken) msg.getPayload()).getCount());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					//tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getFishId());
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFishId());
				}

				if (msg.getPayload() instanceof NameResolutionResponse) {
					tankModel.sendLocationUpdate(((NameResolutionResponse) msg.getPayload()).getTankAdress(), ((NameResolutionResponse) msg.getPayload()).getRequestId());
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					tankModel.updateFishAddress(((LocationUpdate) msg.getPayload()).getFishId(), msg.getSender());
				}

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
