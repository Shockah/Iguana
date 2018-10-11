package pl.shockah.iguana.command;

import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

import pl.shockah.iguana.bridge.IrcServerBridge;

public interface ServerCommand extends Command {
	void execute(@Nonnull IrcServerBridge server, @Nonnull Message executingMessage, @Nonnull String input);

	@Nonnull
	default ChannelCommand asChannel() {
		return (channel, executingMessage, input) -> execute(channel.getServerBridge(), executingMessage, input);
	}
}