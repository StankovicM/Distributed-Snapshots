package snapshot.ab;

import servent.message.Message;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ABSnapshotResult implements Serializable {

    private static final long serialVersionUID = 940094527878590406L;

    private final int serventId;
    private final int recordedAmount;
    private final Map<Integer, List<Message>> sent;
    private final Map<Integer, List<Message>> recd;

    public ABSnapshotResult(int serventId, int recordedAmount, Map<Integer, List<Message>> sent, Map<Integer, List<Message>> recd) {

        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        this.sent = new ConcurrentHashMap<>(sent);
        this.recd = new ConcurrentHashMap<>(recd);

    }

    public int getServentId() { return serventId; }

    public int getRecordedAmount() { return recordedAmount; }

    public Map<Integer, List<Message>> getSent() { return sent; }

    public Map<Integer, List<Message>> getRecd() { return recd; }

}
