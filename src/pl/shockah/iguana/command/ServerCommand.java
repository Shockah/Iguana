package pl.shockah.iguana.command;

import javax.annotation.Nonnull;

import pl.shockah.iguana.bridge.IrcServerBridge;

public interface ServerCommand extends Command {
	void execute(@Nonnull IrcServerBridge server, @Nonnull String input);

	@Nonnull
	default ChannelCommand asChannel() {
		return (channel, input) -> execute(channel.getServerBridge(), input);
	}
}