package snapshot.ab.message;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import snapshot.ab.ABSnapshotResult;
import snapshot.shared.message.CausalMessage;

import java.util.List;
import java.util.Map;

public class ABResultMessage extends CausalMessage {

    private static final long serialVersionUID = -7529374084192942310L;

    private final ABSnapshotResult result;

    public ABResultMessage(ServentInfo senderInfo, ServentInfo receiverInfo,
                           Map<Integer, Integer>  senderVectorClock, ABSnapshotResult result) {

        super(MessageType.AB_RESULT, senderInfo, receiverInfo, "", senderVectorClock);

        this.result = result;

    }

    private ABResultMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                            List<ServentInfo> routeList, String messageText,
                            int messageId, Map<Integer, Integer> senderVectorClock,
                            ABSnapshotResult result) {

        super(MessageType.AB_RESULT, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

        this.result = result;

    }

    public ABSnapshotResult getResult() { return result; }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new ABResultMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock(), getResult());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new ABResultMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock(), getResult());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

    @Override
    public void sendEffect() {}

}
