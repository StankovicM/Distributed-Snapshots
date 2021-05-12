package snapshot.av;

import app.AppConfig;
import servent.message.Message;
import snapshot.shared.BitcakeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AVBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private int snapshotResult = 0;

    private static final Object channelLock = new Object();
    private Map<Integer, List<Message>> channelMessages = new ConcurrentHashMap<>();

    public AVBitcakeManager() {

        synchronized (channelLock) {
            for (int i = 0; i < AppConfig.getServentCount(); i++) {
                channelMessages.put(i, new ArrayList<>());
            }
        }

    }

    public void takeSomeBitcakes(int amount) { currentAmount.getAndAdd(-amount); }

    public void addSomeBitcakes(int amount) { currentAmount.getAndAdd(amount); }

    public int getCurrentBitcakeAmount() { return currentAmount.get(); }

    public void recordSnapshotResult() { this.snapshotResult = currentAmount.get(); }

    public int getSnapshotResult() { return snapshotResult; }

    public void recordChannelMessage(int id, Message message) {

        synchronized (channelLock) {
            channelMessages.get(id).add(message);
        }

    }

    public Map<Integer, List<Message>> getChannelMessages() {

        return channelMessages;

    }

    public void clearSnapshotResults() {

        snapshotResult = 0;
        synchronized (channelLock) {
            for (Map.Entry<Integer, List<Message>> m : channelMessages.entrySet()) {
                m.getValue().clear();
            }
        }

    }

}
