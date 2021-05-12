package snapshot.shared.handler;

import servent.handler.MessageHandler;
import servent.message.Message;
import snapshot.shared.CausalShared;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private CausalShared causalShared;

	private static Set<Message> receivedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public TransactionHandler(Message clientMessage, CausalShared causalShared) {

		this.clientMessage = clientMessage;
		this.causalShared = causalShared;

	}

	@Override
	public void run() {

		/*
		//Proverimo da li je poruka TRANSACTION
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			//Proverimo da li smo mi poslali ovu poruku
			if (clientMessage.getOriginalSenderInfo().getId() != AppConfig.myServentInfo.getId()) {
				//Proverimo da li smo vec primili ovu poruku
				boolean isMessageNew = receivedMessages.add(clientMessage);

				if (isMessageNew) {
					ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
							clientMessage.getOriginalSenderInfo() :
							clientMessage.getRoute().get(clientMessage.getRoute().size() - 1);

					//Rebrodkastujemo poruku
					AppConfig.timestampedStandardPrint("Rebroadcasting " + clientMessage.getMessageType() + " message.");
					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
						if (lastSenderInfo.getId() == neighbor)
							continue;

						MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
					}

					//Prosledimo je u SharedHandler na obradu
					sharedHandler.addPendingMessage(clientMessage);
					sharedHandler.checkPendingMessages();
				} else {
					AppConfig.timestampedStandardPrint("Already had this " + clientMessage.getMessageType() + " message. No rebroadcast.");
				}
			} else {
				AppConfig.timestampedStandardPrint("Got own " + clientMessage.getMessageType() + " message back. No rebroadcast.");
			}
		} else {
			AppConfig.timestampedErrorPrint(this.getClass().getName() + " got: " + clientMessage);
		}
		 */

	}

}
