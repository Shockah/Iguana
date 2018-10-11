package pl.shockah.iguana.command;

import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

import pl.shockah.iguana.bridge.IrcChannelBridge;

public interface ChannelCommand extends Command {
	void execute(@Nonnull IrcChannelBridge channel, @Nonnull Message executingMessage, @Nonnull String input);
}