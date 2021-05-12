package snapshot.shared;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;
import snapshot.ab.ABBitcakeManager;
import snapshot.ab.ABSnapshotResult;
import snapshot.ab.message.ABResultMessage;
import snapshot.av.AVBitcakeManager;
import snapshot.av.message.AVDoneMessage;
import snapshot.shared.message.CausalMessage;
import snapshot.shared.message.TransactionMessage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CausalShared {

    public static SnapshotCollector snapshotCollector;

    private static final Object pendingMessagesLock = new Object();
    private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();

    private static List<Message> committedMessages = new CopyOnWriteArrayList<>();;

    private static AtomicBoolean collecting = new AtomicBoolean(false);

    private static final Object avLock = new Object();
    private static Map<Integer, Integer> avReferenceClock = null;

    public static void addPendingMessage(Message message) { pendingMessages.add(message); }

    public static void commitMessage(Message message) {

        committedMessages.add(message);
        VectorClock.increment(message.getOriginalSenderInfo().getId());

        checkPendingMessages();

    }

    public static void avStartCollecting(Map<Integer, Integer> referenceClock) {

        AppConfig.timestampedStandardPrint("Snapshot initiated");

        ((AVBitcakeManager) snapshotCollector.getBitcakeManager()).recordSnapshotResult();

        synchronized (avLock) {
            avReferenceClock = new ConcurrentHashMap<>();

            for (Map.Entry<Integer, Integer> m : referenceClock.entrySet()) {
                avReferenceClock.put(m.getKey(), m.getValue());
            }
        }
        collecting.set(true);

    }

    public static void avStopCollecting() {

        collecting.set(false);
        synchronized (avLock) {
            avReferenceClock.clear();
        }

        AVBitcakeManager avBitcakeManager = (AVBitcakeManager) snapshotCollector.getBitcakeManager();
        AppConfig.timestampedStandardPrint("Bitcake count: " + avBitcakeManager.getSnapshotResult());

        //TODO sabrati kanalne poruke
        //TODO ne rade lepo ove kanalne poruke
        for (Map.Entry<Integer, List<Message>> m : avBitcakeManager.getChannelMessages().entrySet()) {
            String text = String.format("Channel %s contains %s transaction messages", m.getKey(), m.getValue().size());
            AppConfig.timestampedStandardPrint(text);
        }

    }

    public static void checkPendingMessages() {

        boolean gotWork = true;

        while (gotWork) {
            gotWork = false;

            synchronized (pendingMessagesLock) {
                Iterator<Message> iterator = pendingMessages.iterator();

                //Paziti da ne dodje do deadlocka
                Map<Integer, Integer> myVectorClock = VectorClock.getClock();
                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    CausalMessage causalPendingMessage = (CausalMessage) pendingMessage;

                    if (!VectorClock.otherClockGreater(myVectorClock, causalPendingMessage.getSenderVectorClock())) {
                        gotWork = true;

                        if (causalPendingMessage.getMessageType() == MessageType.TRANSACTION) {
                            TransactionMessage transactionMessage = (TransactionMessage) causalPendingMessage;

                            //Proverimo da li je transakcija upucena nama i ako jeste izvrsimo je
                            if (transactionMessage.getOriginalReciverId() == AppConfig.myServentInfo.getId()) {
                                AppConfig.timestampedStandardPrint("Processing transaction: " + transactionMessage);

                                String amountString = transactionMessage.getMessageText();

                                int amountNumber = 0;
                                try {
                                    amountNumber = Integer.parseInt(amountString);
                                } catch (NumberFormatException e) {
                                    AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
                                    return;
                                }

                                snapshotCollector.getBitcakeManager().addSomeBitcakes(amountNumber);

                                if (snapshotCollector.getSnapshotType() == SnapshotType.AB) {
                                    //Ako je algoritam AB onda poruku belezimo u bitcake menadzeru
                                    ABBitcakeManager abBitcakeManager = (ABBitcakeManager) snapshotCollector.getBitcakeManager();
                                    abBitcakeManager.recordRecdTransaction(transactionMessage.getOriginalSenderInfo().getId(), transactionMessage);
                                } else if (snapshotCollector.getSnapshotType() == SnapshotType.AV) {
                                    //Ako je algoritam AV proverimo da li je u toku snimanje stanja
                                    if (collecting.get()) {
                                        //Proverimo da li je casovnik transakcije stariji od casovnika tokena
                                        AppConfig.timestampedStandardPrint(causalPendingMessage.getSenderVectorClock().toString() + " " + avReferenceClock.toString());

                                        if (VectorClock.otherClockGreater(transactionMessage.getSenderVectorClock(), avReferenceClock)) {
                                            //Ukljucujemo poruku u kanalne poruke
                                            ((AVBitcakeManager) snapshotCollector.getBitcakeManager())
                                                    .recordChannelMessage(transactionMessage.getOriginalSenderInfo().getId(),
                                                            transactionMessage);
                                        }
                                    }
                                } else {
                                    throw new IllegalStateException("Unrecognized snapshot type.");
                                }
                            }
                        } else if (causalPendingMessage.getMessageType() == MessageType.AB_TOKEN) {
                            ABBitcakeManager abBitcakeManager = (ABBitcakeManager) snapshotCollector.getBitcakeManager();

                            //Belezimo svoj rezultat
                            ABSnapshotResult myResult = new ABSnapshotResult(AppConfig.myServentInfo.getId(),
                                    abBitcakeManager.getCurrentBitcakeAmount(),
                                    abBitcakeManager.getSent(),
                                    abBitcakeManager.getRecd());

                            //Brodkastujemo rezultat
                            Message resultMessage = new ABResultMessage(AppConfig.myServentInfo, null, myVectorClock, myResult);
                            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                                resultMessage = resultMessage.changeReceiver(neighbor);

                                MessageUtil.sendMessage(resultMessage);
                            }

                            //Komitujemo lokalno rezultat
                            Message resultCommitMessage = new ABResultMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, myVectorClock, myResult);
                            committedMessages.add(resultCommitMessage);
                            VectorClock.increment(resultCommitMessage.getOriginalSenderInfo().getId());
                        } else if (causalPendingMessage.getMessageType() == MessageType.AB_RESULT) {
                            if (snapshotCollector.isCollecting()) {
                                ABResultMessage resultMessage = (ABResultMessage) causalPendingMessage;

                                snapshotCollector.addABSnapshotResult(resultMessage.getOriginalSenderInfo().getId(), resultMessage.getResult());
                            }
                        } else if (causalPendingMessage.getMessageType() == MessageType.AV_TOKEN) {
                            if (!collecting.get()) {
                                //Zapocinjemo snimanje stanja
                                avStartCollecting(causalPendingMessage.getSenderVectorClock());

                                //Brodkastujemo DONE poruku
                                Message doneMessage = new AVDoneMessage(AppConfig.myServentInfo, null, myVectorClock);
                                for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                                    doneMessage = doneMessage.changeReceiver(neighbor);

                                    MessageUtil.sendMessage(doneMessage);
                                }

                                //Komitujemo lokalno DONE poruku
                                Message doneCommitMessage = new AVDoneMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, myVectorClock);
                                committedMessages.add(doneCommitMessage);
                                VectorClock.increment(doneCommitMessage.getOriginalSenderInfo().getId());
                            } else {
                                AppConfig.timestampedErrorPrint("Snapshot is already in progress!");
                            }
                        } else if (causalPendingMessage.getMessageType() == MessageType.AV_DONE) {
                            //Proverimo da li je snimanje stanja u toku
                            if (snapshotCollector.isCollecting()) {
                                //Ako smo mi zapoceli snapshot, zabelezimo DONE poruku
                                snapshotCollector.incrementAVDoneCounter(causalPendingMessage.getMessageId());
                            }
                        } else if (causalPendingMessage.getMessageType() == MessageType.AV_TERMINATE) {
                            //Proverimo da li je snimanje stanja u toku
                            if (collecting.get()) {
                                //Prekidamo snimanje stanja i ispisujemo rezultat
                                avStopCollecting();
                            } else {
                                AppConfig.timestampedErrorPrint("Snapshot hasn't been initiated, but we got a TERMINATE message.");
                            }
                        } else {
                            throw new IllegalStateException("Advanacement made: How did we get here?");
                        }

                        //Komitujemo lokalno poruku
                        committedMessages.add(causalPendingMessage);
                        VectorClock.increment(causalPendingMessage.getOriginalSenderInfo().getId());

                        iterator.remove();

                        break;
                    }
                }
            }
        }

    }

}
