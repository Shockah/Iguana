package pl.shockah.iguana.command;

import javax.annotation.Nonnull;

public interface NamedCommand extends Command {
	@Nonnull
	String getName();
}