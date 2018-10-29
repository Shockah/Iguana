package pl.shockah.iguana.command;

import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

import pl.shockah.iguana.bridge.IrcPrivateBridge;

public interface UserCommand extends Command {
	void execute(@Nonnull IrcPrivateBridge privateBridge, @Nonnull Message executingMessage, @Nonnull String input);
}