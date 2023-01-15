package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

public class ToggleController implements ActionListener {

    private final TankModel tankModel;

    private final String fishId;

    public ToggleController(TankModel tankModel, String fishId) {
        this.tankModel = tankModel;
        this.fishId = fishId;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            tankModel.locateFishGlobally(fishId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }
}
