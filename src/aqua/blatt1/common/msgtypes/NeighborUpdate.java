package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {
    InetSocketAddress leftNeighbor;
    InetSocketAddress rightNeighbor;

    public NeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
        this.leftNeighbor = leftNeighbor;
        this.rightNeighbor = rightNeighbor;
    }

    public InetSocketAddress getLeftNeighbor() {
        return this.leftNeighbor;
    }

    public InetSocketAddress getRightNeighboar() {
        return this.rightNeighbor;
    }
}
