package app;

import snapshot.shared.NullSnapshotCollector;
import snapshot.shared.SnapshotCollector;
import snapshot.shared.SnapshotCollectorWorker;
import snapshot.shared.SnapshotType;
import cli.CLIParser;
import servent.SimpleServentListener;
import snapshot.shared.VectorClock;
import snapshot.shared.CausalShared;

/**
 * Describes the procedure for starting a single Servent
 *
 * @author bmilojkovic
 */
public class ServentMain {

	/**
	 * Command line arguments are:
	 * 0 - path to servent list file
	 * 1 - this servent's id
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
		}
		
		int serventId = -1;
		int portNumber = -1;
		
		String serventListFile = args[0];
		
		AppConfig.readConfig(serventListFile);
		
		try {
			serventId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
			System.exit(0);
		}
		
		if (serventId >= AppConfig.getServentCount()) {
			AppConfig.timestampedErrorPrint("Invalid servent id provided");
			System.exit(0);
		}
		
		AppConfig.myServentInfo = AppConfig.getInfoById(serventId);
		
		try {
			portNumber = AppConfig.myServentInfo.getListenerPort();
			
			if (portNumber < 1000 || portNumber > 2000) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
			System.exit(0);
		}
		
		AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

		SnapshotCollector snapshotCollector;
		
		if (AppConfig.SNAPSHOT_TYPE == SnapshotType.NONE) {
			snapshotCollector = new NullSnapshotCollector();
		} else {
			snapshotCollector = new SnapshotCollectorWorker(AppConfig.SNAPSHOT_TYPE);
		}

		//snapshot
		VectorClock.init(AppConfig.getServentCount());
		CausalShared.snapshotCollector = snapshotCollector;

		Thread snapshotCollectorThread = new Thread(snapshotCollector);
		snapshotCollectorThread.start();
		
		SimpleServentListener simpleListener = new SimpleServentListener(snapshotCollector);
		Thread listenerThread = new Thread(simpleListener);
		listenerThread.start();
		
		CLIParser cliParser = new CLIParser(simpleListener, snapshotCollector);
		Thread cliThread = new Thread(cliParser);
		cliThread.start();
		
	}
}
