package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaClient extends Remote {
    public void receiveToken() throws RemoteException;

    public void receiveFish(FishModel fish) throws RemoteException;

    public void locateFishGlobally(String fishId) throws RemoteException;

    public void locationUpdate(String fishID, AquaClient newLoc) throws RemoteException;

    public void onRegistration(String id) throws RemoteException;

    public void receiveNeighbor(AquaClient l, AquaClient r) throws RemoteException;
}

