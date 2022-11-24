package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class NameResolutionRequest implements Serializable {
    private String tankId;
    private String requestId;

    public NameResolutionRequest(String tankId, String requestId) {
        this.tankId = tankId;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getTankId() {
        return this.tankId;
    }
}
