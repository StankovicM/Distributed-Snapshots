package snapshot.shared.command;

import snapshot.shared.SnapshotCollector;
import cli.command.CLICommand;

public class BitcakeInfoCommand implements CLICommand {

	private SnapshotCollector collector;
	
	public BitcakeInfoCommand(SnapshotCollector collector) {
		this.collector = collector;
	}
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
		collector.startCollecting();

	}

}
