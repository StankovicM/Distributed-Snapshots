package snapshot.shared.command;

import app.AppConfig;
import app.ServentInfo;
import snapshot.shared.BitcakeManager;
import cli.command.CLICommand;
import servent.message.Message;
import snapshot.shared.VectorClock;
import snapshot.shared.CausalShared;
import servent.message.util.MessageUtil;
import snapshot.shared.message.TransactionMessage;

import java.util.Map;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 3;
	private static final int BURST_WORKERS = 2;
	private static final int MAX_TRANSFER_AMOUNT = 10;

//	private static final int TRANSACTION_COUNT = 5;
//	private static final int BURST_WORKERS = 10;
//	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	//Chandy-Lamport
//	private static final int TRANSACTION_COUNT = 3;
//	private static final int BURST_WORKERS = 5;
//	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	private BitcakeManager bitcakeManager;

	public TransactionBurstCommand(BitcakeManager bitcakeManager) {

		this.bitcakeManager = bitcakeManager;

	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);
					
					int amount = 1 + (int)(Math.random() * MAX_TRANSFER_AMOUNT);
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */
					Map<Integer, Integer> myClock = VectorClock.getClock();

					Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo, neighborInfo,
							amount, myClock, neighbor, false, bitcakeManager);
					
					MessageUtil.sendMessage(transactionMessage);
				}
				
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
		}
	}

	
}
