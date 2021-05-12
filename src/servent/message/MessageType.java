package servent.message;

public enum MessageType {
	PING, PONG, POISON, TRANSACTION,
	AB_TOKEN, AB_RESULT,
	AV_TOKEN, AV_DONE, AV_TERMINATE
}
