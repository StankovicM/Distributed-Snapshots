package snapshot.shared;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import servent.message.Message;
import servent.message.util.MessageUtil;
import snapshot.ab.ABBitcakeManager;
import snapshot.ab.ABSnapshotResult;
import snapshot.ab.message.ABTokenMessage;
import snapshot.av.AVBitcakeManager;
import snapshot.av.message.AVTerminateMessage;
import snapshot.av.message.AVTokenMessage;
import snapshot.shared.message.TransactionMessage;

/**
 * Main snapshot collector class.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);

	private SnapshotType snapshotType;
	
	private BitcakeManager bitcakeManager;

	private Map<Integer, ABSnapshotResult> collectedABResults = new ConcurrentHashMap<>();

	private AtomicInteger avDoneCounter = new AtomicInteger(0);

	public SnapshotCollectorWorker(SnapshotType snapshotType) {

		this.snapshotType = snapshotType;
		
		switch(snapshotType) {
		case AB:
			bitcakeManager = new ABBitcakeManager();
			break;
		case AV:
			bitcakeManager = new AVBitcakeManager();
			break;
		case NONE:
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}

	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	@Override
	public SnapshotType getSnapshotType() { return snapshotType; }

	@SuppressWarnings("Duplicates")
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			Map<Integer, Integer> myClock = VectorClock.getClock();
			AppConfig.timestampedStandardPrint("Initiating snapshot and broadcasting TOKEN.");
			switch (snapshotType) {
			case AB:
				//Brodkastujemo TOKEN
				Message abTokenMessage = new ABTokenMessage(AppConfig.myServentInfo, null, myClock);
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					abTokenMessage = abTokenMessage.changeReceiver(neighbor);

					MessageUtil.sendMessage(abTokenMessage);
				}

				//Komitujemo lokalno poslatu poruku
				Message abTokenCommitMessage = new ABTokenMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, myClock);
				CausalShared.commitMessage(abTokenCommitMessage);

				//Belezimo svoj rezultat
				ABSnapshotResult myResult = new ABSnapshotResult(AppConfig.myServentInfo.getId(),
						bitcakeManager.getCurrentBitcakeAmount(),
						((ABBitcakeManager) bitcakeManager).getSent(),
						((ABBitcakeManager) bitcakeManager).getRecd());
				collectedABResults.put(AppConfig.myServentInfo.getId(), myResult);

				break;
			case AV:
				//Brodkastujemo TOKEN
				Message avTokenMessage = new AVTokenMessage(AppConfig.myServentInfo, null, myClock);
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					avTokenMessage = avTokenMessage.changeReceiver(neighbor);

					MessageUtil.sendMessage(avTokenMessage);
				}

				//Komitujemo lokalno poslatu poruku
				Message avTokenCommitMessage = new AVTokenMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, myClock);
				CausalShared.commitMessage(avTokenCommitMessage);

				//Pozivamo metodu koja ce zabeleziti rezultat i povecavamo brojac za sebe
				CausalShared.avStartCollecting(myClock);
				incrementAVDoneCounter(AppConfig.myServentInfo.getId());

				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}

			switch (snapshotType) {
			case AB:
				AppConfig.timestampedStandardPrint("Waiting for results...");
				break;
			case AV:
				AppConfig.timestampedStandardPrint("Waiting for DONE messages...");
				break;
			case NONE:
				break;
			}

			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case AB:
					if (collectedABResults.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case AV:
					if (avDoneCounter.get() == AppConfig.getServentCount()) {
						waiting = false;
					}

					//Dobili smo sve DONE poruke i brodkastujemo TERMINATE poruku
					myClock = VectorClock.getClock();
					Message avTerminateMessage = new AVTerminateMessage(AppConfig.myServentInfo, null, myClock);
					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
						avTerminateMessage = avTerminateMessage.changeReceiver(neighbor);

						MessageUtil.sendMessage(avTerminateMessage);
					}

					//Komitujemo lokalno TERMINATE poruku
					Message avTerminateCommitMessage = new AVTerminateMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, myClock);
					CausalShared.commitMessage(avTerminateCommitMessage);

					break;
				case NONE:
					//Shouldn't be able to come here. See constructor. 
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}

			switch (snapshotType) {
			case AB:
				AppConfig.timestampedStandardPrint("Collected all results.");
				break;
			case AV:
				AppConfig.timestampedStandardPrint("Got all done messages.");
				break;
			case NONE:
				break;
			}
			
			//print
			int sum;
			switch (snapshotType) {
			case AB:
				sum = 0;
				for (Map.Entry<Integer, ABSnapshotResult> idResult : collectedABResults.entrySet()) {
					sum += idResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Info for " + idResult.getKey() + " = " + idResult.getValue().getRecordedAmount() + " bitcake");
				}

				for(int i = 0; i < AppConfig.getServentCount(); i++) {
					for (int j = 0; j < AppConfig.getServentCount(); j++) {
						if (i != j) {
							if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
									AppConfig.getInfoById(j).getNeighbors().contains(i)) {
								List<Message> ijMessages = collectedABResults.get(i).getSent().get(j);
								List<Message> jiMessages = collectedABResults.get(j).getRecd().get(i);

								int ijAmount = 0;
								for (Message m : ijMessages) {
									TransactionMessage t = null;
									try {
										t = (TransactionMessage) m;
									} catch (ClassCastException e) {
										e.printStackTrace();
									}

									ijAmount += Integer.parseInt(t.getMessageText());
								}

								int jiAmount = 0;
								for (Message m : jiMessages) {
									TransactionMessage t = null;
									try {
										t = (TransactionMessage) m;
									} catch (ClassCastException e) {
										e.printStackTrace();
									}

									jiAmount += Integer.parseInt(t.getMessageText());
								}

								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedStandardPrint(outputString);
									sum += ijAmount - jiAmount;
								}
							}
						}
					}
				}

				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

				collectedABResults.clear();
				break;
			case AV:
				CausalShared.avStopCollecting();

				avDoneCounter.set(0);
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}

			collecting.set(false);
		}

	}

	@Override
	public void addABSnapshotResult(int id, ABSnapshotResult result) {

		collectedABResults.put(id, result);

		String text = String.format("Got a result from %s [%s/%s]", id, collectedABResults.size(), AppConfig.getServentCount());
		AppConfig.timestampedStandardPrint(text);

	}

	@Override
	public void incrementAVDoneCounter(int id) {

		int doneCounter = avDoneCounter.incrementAndGet();

		String text = String.format("Got a DONE message from %s [%s/%s]", id, doneCounter, AppConfig.getServentCount());
		AppConfig.timestampedStandardPrint(text);

	}

	@Override
	public void startCollecting() {

		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}

	}

	@Override
	public boolean isCollecting() { return collecting.get(); }

	@Override
	public void stop() {
		working = false;
	}

}
