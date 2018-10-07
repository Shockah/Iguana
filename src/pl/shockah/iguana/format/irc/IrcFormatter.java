package pl.shockah.iguana.format.irc;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.experimental.Delegate;
import pl.shockah.iguana.format.Formatter;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.FormattingParser;

public class IrcFormatter implements Formatter<Void, Void, String> {
	@Nonnull
	@Getter
	@Delegate
	private final FormattingParser<Void> parser = new IrcFormattingParser();

	@Nonnull
	@Getter
	@Delegate
	private final FormattingOutputer<Void, String> outputer = new IrcFormattingOutputer();
}