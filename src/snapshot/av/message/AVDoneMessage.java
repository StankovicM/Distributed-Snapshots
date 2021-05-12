package snapshot.av.message;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import snapshot.ab.message.ABTokenMessage;
import snapshot.shared.message.CausalMessage;

import java.util.List;
import java.util.Map;

public class AVDoneMessage extends CausalMessage {

    private static final long serialVersionUID = 8734110238067932986L;

    public AVDoneMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_DONE, senderInfo, receiverInfo, "", senderVectorClock);

    }

    private AVDoneMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                         List<ServentInfo> routeList, String messageText,
                         int messageId, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_DONE, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new AVDoneMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AVDoneMessage(getOriginalSenderInfo(), newReceiverInfo,
                    getRoute(), getMessageText(), getMessageId(), getSenderVectorClock());

            return toReturn;
        } else {
            AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
            return null;
        }

    }

    @Override
    public void sendEffect() {}

}
