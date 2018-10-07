package pl.shockah.iguana.format.discord;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.experimental.Delegate;
import pl.shockah.iguana.format.Formatter;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.FormattingParser;

public class DiscordFormatter implements Formatter<Void, Void> {
	@Nonnull
	@Getter
	@Delegate
	private final FormattingParser<Void> parser = new DiscordFormattingParser();

	@Nonnull
	@Getter
	@Delegate
	private final FormattingOutputer<Void> outputer = new DiscordFormattingOutputer();
}