package snapshot.ab;

import app.AppConfig;
import snapshot.shared.BitcakeManager;
import servent.message.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ABBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private static final Object sentLock = new Object();
    private static Map<Integer, List<Message>> sent = new ConcurrentHashMap<>();

    private static final Object recdLock = new Object();
    private static Map<Integer, List<Message>> recd = new ConcurrentHashMap<>();

    public ABBitcakeManager() {

        for (int i = 0; i < AppConfig.getServentCount(); i++) {
            sent.put(i, new CopyOnWriteArrayList<>());
            recd.put(i, new CopyOnWriteArrayList<>());
        }

    }

    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void recordSentTransaction(int serventId, Message message) {

        synchronized (sentLock) {
            sent.get(serventId).add(message);
        }

    }

    public void recordRecdTransaction(int serventId, Message message) {

        synchronized (recdLock) {
            recd.get(serventId).add(message);
        }

    }

    @SuppressWarnings("Duplicates")
    public Map<Integer, List<Message>> getSent() {

        Map<Integer, List<Message>> toReturn = new ConcurrentHashMap<>();
        synchronized (sentLock) {
            for (Map.Entry<Integer, List<Message>> m : sent.entrySet()) {
                toReturn.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
            }
        }

        return toReturn;

    }

    @SuppressWarnings("Duplicates")
    public Map<Integer, List<Message>> getRecd() {

        Map<Integer, List<Message>> toReturn = new ConcurrentHashMap<>();
        synchronized (recdLock) {
            for (Map.Entry<Integer, List<Message>> m : recd.entrySet()) {
                toReturn.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
            }
        }

        return toReturn;

    }

}
