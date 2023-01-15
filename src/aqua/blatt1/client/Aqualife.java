package aqua.blatt1.client;

import aqua.blatt1.broker.AquaBroker;
import aqua.blatt1.common.Properties;

import javax.swing.SwingUtilities;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Aqualife {
	public static void main(String[] args) throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry();
		AquaBroker broker = (AquaBroker) registry.lookup(Properties.BROKER_NAME);
		TankModel tankModel = new TankModel(broker);

		SwingUtilities.invokeLater(new AquaGui(tankModel));

		tankModel.run();
	}
}
