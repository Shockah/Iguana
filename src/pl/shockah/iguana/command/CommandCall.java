package pl.shockah.iguana.command;

import javax.annotation.Nonnull;

public final class CommandCall {
	@Nonnull
	public final String commandName;

	@Nonnull
	public final String input;

	public CommandCall(@Nonnull String commandName, @Nonnull String input) {
		this.commandName = commandName;
		this.input = input;
	}
}