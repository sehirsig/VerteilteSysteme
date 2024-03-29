package aqua.blatt2.broker;

import java.net.InetSocketAddress;

import javax.swing.JOptionPane;

import aqua.blatt1.common.security.SecureEndpoint;
import messaging.Endpoint;
import aqua.blatt1.common.Properties;

public class Poisoner {
	private final Endpoint endpoint;
	private final InetSocketAddress broker;

	public Poisoner() {
		this.endpoint = new SecureEndpoint();
		this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
	}

	public void sendPoison() {
		endpoint.send(broker, new PoisonPill());
	}

	public static void main(String[] args) {
		JOptionPane.showMessageDialog(null, "Press OK button to poison server.");
		new Poisoner().sendPoison();
	}
}
