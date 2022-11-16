package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.FishModel;

import java.io.Serializable;

public class SnapshotToken implements Serializable {
    int count;

    public SnapshotToken(int count) {
        this.count = count;
    }

    public int getCount() {
        return this.count;
    }
}
