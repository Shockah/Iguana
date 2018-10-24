package pl.shockah.iguana.format.discord;

import java.awt.image.BufferedImage;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.experimental.Delegate;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.format.Formatter;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.FormattingParser;
import pl.shockah.unicorn.collection.Either2;

public class DiscordFormatter implements Formatter<Void, Void, Either2<String, BufferedImage>> {
	@Nonnull
	@Getter
	@Delegate
	private final FormattingParser<Void> parser = new DiscordFormattingParser();

	@Nonnull
	@Getter
	@Delegate
	private final FormattingOutputer<Void, Either2<String, BufferedImage>> outputer;

	public DiscordFormatter(@Nonnull Configuration.Appearance appearanceConfiguration) {
		outputer = new DiscordFormattingOutputer(appearanceConfiguration);
	}
}