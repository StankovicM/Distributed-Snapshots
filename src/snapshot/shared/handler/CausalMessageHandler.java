package snapshot.shared.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.util.MessageUtil;
import snapshot.shared.CausalShared;
import snapshot.shared.message.CausalMessage;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CausalMessageHandler implements MessageHandler {

    private static final boolean MESSAGE_UTIL_PRINTING = false;

    private final Message clientMessage;

    private static Set<Message> receivedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CausalMessageHandler(Message clientMessage) {

        this.clientMessage = clientMessage;

    }

    @Override
    public void run() {

        //Proverimo da li je poruka kauzalna
        if (clientMessage instanceof CausalMessage) {
            //Proverimo da li smo mi poslali ovu poruku
            if (clientMessage.getOriginalSenderInfo().getId() != AppConfig.myServentInfo.getId()) {
                //Proverimo da li smo vec primili ovu poruku
                boolean isMessageNew = receivedMessages.add(clientMessage);

                if (isMessageNew) {
                    ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
                            clientMessage.getOriginalSenderInfo() :
                            clientMessage.getRoute().get(clientMessage.getRoute().size() - 1);

                    //Rebrodkastujemo poruku
                    if (MESSAGE_UTIL_PRINTING)
                        AppConfig.timestampedStandardPrint("Rebroadcasting " + clientMessage.getMessageType() + " message.");

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        if (lastSenderInfo.getId() == neighbor)
                            continue;

                        MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
                    }

                    //Prosledimo je u SharedHandler na obradu
                    CausalShared.addPendingMessage(clientMessage);
                    CausalShared.checkPendingMessages();
                } else {
                    if (MESSAGE_UTIL_PRINTING)
                        AppConfig.timestampedStandardPrint("Already had this " + clientMessage.getMessageType() + " message. No rebroadcast.");
                }
            } else {
                if (MESSAGE_UTIL_PRINTING)
                    AppConfig.timestampedStandardPrint("Got own " + clientMessage.getMessageType() + " message back. No rebroadcast.");
            }
        } else {
            AppConfig.timestampedErrorPrint(this.getClass().getName() + " got: " + clientMessage);
        }

    }

}
