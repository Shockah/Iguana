package pl.shockah.iguana.format.discord;

import java.util.List;

import javax.annotation.Nonnull;

import javafx.scene.image.Image;
import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.IrcColor;
import pl.shockah.unicorn.collection.Either2;

public class DiscordFormattingOutputer implements FormattingOutputer<Void, Either2<String, Image>> {
	@Nonnull
	private String escapeFormatting(@Nonnull String text) {
		return text.replaceAll("([*_~`\\\\])", "\\\\$1");
	}

	@Nonnull
	@Override
	public Either2<String, Image> output(@Nonnull List<FormattedString> formattedStrings, Void context) {
		boolean returnImage = formattedStrings.stream()
				.anyMatch(string -> string.textColor != IrcColor.Default || string.backgroundColor != IrcColor.Default);

		if (returnImage)
			return Either2.second(outputImage(formattedStrings, context));
		else
			return Either2.first(outputString(formattedStrings, context));
	}

	@Nonnull
	private String outputString(@Nonnull List<FormattedString> formattedStrings, Void context) {
		StringBuilder sb = new StringBuilder();

		for (FormattedString string : formattedStrings) {
			if (string.underline)
				sb.append("__");
			if (string.strikethrough)
				sb.append("~~");

			if (string.bold && string.italic)
				sb.append("***");
			else if (string.bold)
				sb.append("**");
			else if (string.italic)
				sb.append("*");

			sb.append(escapeFormatting(string.text));

			if (string.bold && string.italic)
				sb.append("***");
			else if (string.bold)
				sb.append("**");
			else if (string.italic)
				sb.append("*");

			if (string.strikethrough)
				sb.append("~~");
			if (string.underline)
				sb.append("__");
		}

		return sb.toString();
	}

	@Nonnull
	private Image outputImage(@Nonnull List<FormattedString> formattedStrings, Void context) {
		// TODO: implementation
		throw new UnsupportedOperationException();
	}
}