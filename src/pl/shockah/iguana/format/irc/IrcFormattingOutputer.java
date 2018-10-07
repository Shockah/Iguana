package pl.shockah.iguana.format.irc;

import java.util.List;

import javax.annotation.Nonnull;

import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.IrcColor;

public class IrcFormattingOutputer implements FormattingOutputer<Void, String> {
	@Nonnull
	@Override
	public String output(@Nonnull List<FormattedString> formattedStrings, Void aVoid) {
		StringBuilder sb = new StringBuilder();

		FormattedString lastString = new FormattedString("");

		for (FormattedString string : formattedStrings) {
			StringBuilder toggleVariant = new StringBuilder();
			StringBuilder resetVariant = new StringBuilder(IrcFormattingConstants.RESET);

			if (string.bold != lastString.bold)
				toggleVariant.append(IrcFormattingConstants.BOLD);
			if (string.bold)
				resetVariant.append(IrcFormattingConstants.BOLD);

			if (string.italic != lastString.italic)
				toggleVariant.append(IrcFormattingConstants.ITALIC);
			if (string.italic)
				resetVariant.append(IrcFormattingConstants.ITALIC);

			if (string.underline != lastString.underline)
				toggleVariant.append(IrcFormattingConstants.UNDERLINE);
			if (string.underline)
				resetVariant.append(IrcFormattingConstants.UNDERLINE);

			if (string.strikethrough != lastString.strikethrough)
				toggleVariant.append(IrcFormattingConstants.STRIKETHROUGH);
			if (string.strikethrough)
				resetVariant.append(IrcFormattingConstants.STRIKETHROUGH);

			if (string.inverse != lastString.inverse)
				toggleVariant.append(IrcFormattingConstants.INVERSE);
			if (string.inverse)
				resetVariant.append(IrcFormattingConstants.INVERSE);

			if (string.textColor != lastString.textColor || string.backgroundColor != lastString.backgroundColor)
				toggleVariant.append(IrcFormattingConstants.getColorPrefix(string.textColor, string.backgroundColor, string.text));
			if (string.textColor != IrcColor.Default || string.backgroundColor != IrcColor.Default)
				resetVariant.append(IrcFormattingConstants.getColorPrefix(string.textColor, string.backgroundColor, string.text));

			if (toggleVariant.length() < resetVariant.length())
				sb.append(toggleVariant);
			else
				sb.append(resetVariant);

			sb.append(string.text);
		}

		return sb.toString();
	}
}