package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private InetSocketAddress tankAdress;
    private String requestId;

    public NameResolutionResponse(InetSocketAddress tankAdress, String requestId) {
        this.tankAdress = tankAdress;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public InetSocketAddress getTankId() {
        return this.tankAdress;
    }
}
