package snapshot.shared;

import app.Cancellable;
import snapshot.ab.ABSnapshotResult;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();

	SnapshotType getSnapshotType();

	void addABSnapshotResult(int id, ABSnapshotResult result);

	void incrementAVDoneCounter(int id);

	void startCollecting();

	boolean isCollecting();

}