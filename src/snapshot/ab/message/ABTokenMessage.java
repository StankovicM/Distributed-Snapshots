package snapshot.ab.message;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import snapshot.shared.message.CausalMessage;

import java.util.List;
import java.util.Map;

public class ABTokenMessage extends CausalMessage {

    private static final long serialVersionUID = 2069242760513950171L;

    public ABTokenMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AB_TOKEN, senderInfo, receiverInfo, "", senderVectorClock);

    }

    private ABTokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                           List<ServentInfo> routeList, String messageText,
                           int messageId, Map<Integer, Integer> senderVectorClock) {

        super(MessageType.AB_TOKEN, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

    }

    @Override
    public Message makeMeASender() {

        ServentInfo myInfo = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = getRoute();
        newRouteList.add(myInfo);
        Message toReturn = new ABTokenMessage(getOriginalSenderInfo(), getReceiverInfo(),
                newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());

        return toReturn;

    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {

        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
            Message toReturn = new ABTokenMessage(getOriginalSenderInfo(), newReceiverInfo,
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
