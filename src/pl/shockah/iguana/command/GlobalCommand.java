package pl.shockah.iguana.command;

import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

import pl.shockah.iguana.IguanaSession;

public interface GlobalCommand extends Command {
	void execute(@Nonnull IguanaSession session, @Nonnull Message executingMessage, @Nonnull String input);

	@Nonnull
	default ServerCommand asServer() {
		return (server, executingMessage, input) -> execute(server.getSession(), executingMessage, input);
	}

	@Nonnull
	default ChannelCommand asChannel() {
		return (channel, executingMessage, input) -> execute(channel.getSession(), executingMessage, input);
	}
}