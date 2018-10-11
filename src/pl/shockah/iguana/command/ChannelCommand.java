package pl.shockah.iguana.command;

import javax.annotation.Nonnull;

import pl.shockah.iguana.bridge.IrcChannelBridge;

public interface ChannelCommand extends Command {
	void execute(@Nonnull IrcChannelBridge channel, @Nonnull String input);
}