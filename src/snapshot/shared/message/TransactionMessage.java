package snapshot.shared.message;

import app.AppConfig;
import app.ServentInfo;
import snapshot.ab.ABBitcakeManager;
import snapshot.av.AVBitcakeManager;
import snapshot.shared.BitcakeManager;
import servent.message.Message;
import servent.message.MessageType;
import snapshot.shared.CausalShared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionMessage extends CausalMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private final int originalReciverId;
	private final boolean isRebroadcast;

	private transient BitcakeManager bitcakeManager;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount,
							  Map<Integer, Integer> senderVectorClock, int originalReceiverId,
							  boolean isRebroadcast, BitcakeManager bitcakeManager) {

		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount), senderVectorClock);

		this.originalReciverId = originalReceiverId;
		this.isRebroadcast = isRebroadcast;

		this.bitcakeManager = bitcakeManager;

	}

	private TransactionMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
							   List<ServentInfo> routeList, String messageText,
							   int messageId, Map<Integer, Integer> senderVectorClock,
							   int originalReciverId, boolean isRebroadcast) {

		super(MessageType.TRANSACTION, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

		this.originalReciverId = originalReciverId;
		this.isRebroadcast = isRebroadcast;

	}

	public int getOriginalReciverId() { return originalReciverId; }

	@Override
	public Message makeMeASender() {

		ServentInfo myInfo = AppConfig.myServentInfo;
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(myInfo);
		Message toReturn = new TransactionMessage(getOriginalSenderInfo(), getReceiverInfo(), newRouteList,
				getMessageText(), getMessageId(), getSenderVectorClock(), getOriginalReciverId(), true);

		return toReturn;

	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {

		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			Message toReturn = new TransactionMessage(getOriginalSenderInfo(), newReceiverInfo,
					getRoute(), getMessageText(), getMessageId(), getSenderVectorClock(),
					getOriginalReciverId(), true);

			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
			return null;
		}

	}

	@Override
	public void sendEffect() {

		if (!isRebroadcast) {
			int amount = Integer.parseInt(getMessageText());

			bitcakeManager.takeSomeBitcakes(amount);

			CausalShared.commitMessage(this);

			if (bitcakeManager instanceof ABBitcakeManager) {
				((ABBitcakeManager) bitcakeManager).recordSentTransaction(getOriginalReciverId(), this);
			}
		}

	}

	@Override
	public String toString() {

		return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
				getOriginalSenderInfo().getId() + "--(" + getMessageText() + ")-->" + getOriginalReciverId() +
				"|" + getMessageType() + "|" + getReceiverInfo().getId() + "]";

	}

}
