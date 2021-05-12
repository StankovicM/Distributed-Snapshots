package snapshot.av.message;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import snapshot.ab.message.ABTokenMessage;
import snapshot.shared.message.CausalMessage;

import java.util.List;
import java.util.Map;

public class AVTokenMessage extends CausalMessage {

    private static final long serialVersionUID = -3261799463147177616L;

    public AVTokenMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_TOKEN, senderInfo, receiverInfo, "", senderVectorClock);

    }

    public AVTokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                          List<ServentInfo> routeList, String messageText,
                          int messageId, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AV_TOKEN, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new AVTokenMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new AVTokenMessage(getOriginalSenderInfo(), newReceiverInfo,
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
