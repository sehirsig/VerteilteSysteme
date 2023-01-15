package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.FishModel;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaBroker extends Remote {

    void register(AquaClient client) throws RemoteException;

    void deregister(AquaClient client) throws RemoteException;

    void resoluteId(AquaClient client, FishModel fish) throws RemoteException;
}
