package pl.shockah.iguana.command;

import javax.annotation.Nonnull;

import pl.shockah.iguana.IguanaSession;

public interface GlobalCommand extends Command {
	void execute(@Nonnull IguanaSession session, @Nonnull String input);

	@Nonnull
	default ServerCommand asServer() {
		return (server, input) -> execute(server.getSession(), input);
	}

	@Nonnull
	default ChannelCommand asChannel() {
		return (channel, input) -> execute(channel.getSession(), input);
	}
}