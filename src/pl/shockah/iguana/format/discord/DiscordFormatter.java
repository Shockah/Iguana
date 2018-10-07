package pl.shockah.iguana.format.discord;

import javax.annotation.Nonnull;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.experimental.Delegate;
import pl.shockah.iguana.format.Formatter;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.FormattingParser;
import pl.shockah.unicorn.collection.Either2;

public class DiscordFormatter implements Formatter<Void, Void, Either2<String, Image>> {
	@Nonnull
	@Getter
	@Delegate
	private final FormattingParser<Void> parser = new DiscordFormattingParser();

	@Nonnull
	@Getter
	@Delegate
	private final FormattingOutputer<Void, Either2<String, Image>> outputer = new DiscordFormattingOutputer();
}