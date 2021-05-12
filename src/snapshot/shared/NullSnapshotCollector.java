package snapshot.shared;

import snapshot.ab.ABSnapshotResult;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public SnapshotType getSnapshotType() { return null; }

	@Override
	public void addABSnapshotResult(int id, ABSnapshotResult result) {}

	@Override
	public void incrementAVDoneCounter(int id) {}

	@Override
	public void startCollecting() {}

	@Override
	public boolean isCollecting() { return false; }

}
