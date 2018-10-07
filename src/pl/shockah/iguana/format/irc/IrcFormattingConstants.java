package pl.shockah.iguana.format.irc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;
import pl.shockah.iguana.format.IrcColor;

@UtilityClass
public final class IrcFormattingConstants {
	@Nonnull
	public static final String RESET = "\u000f";

	@Nonnull
	public static final String BOLD = "\u0002";

	@Nonnull
	public static final String ITALIC = "\u001d";

	@Nonnull
	public static final String UNDERLINE = "\u001f";

	@Nonnull
	public static final String STRIKETHROUGH = "\u001e";

//	@Nonnull
//	public static final String MONOSPACE = "\u0011";

	@Nonnull
	public static final String INVERSE = "\u0016";

	@Nonnull
	protected static final String COLOR_PREFIX = "\u0003";

	@Nonnull
	public static String getColorPrefix(@Nullable IrcColor textColor, @Nullable IrcColor backgroundColor, boolean nextCharacterADigit) {
		if (textColor == null && backgroundColor == null) {
			return "";
		} else {
			if (nextCharacterADigit) {
				if (textColor == null || backgroundColor == null) {
					throw new IllegalArgumentException("Both colors have to be set for the given `text` value.");
				} else {
					return String.format("%s%s,%s", COLOR_PREFIX, textColor.code, backgroundColor.code);
				}
			} else {
				if (textColor == IrcColor.Default && backgroundColor == IrcColor.Default) {
					return COLOR_PREFIX;
				} else if (backgroundColor == null) {
					return String.format("%s%s", COLOR_PREFIX, textColor.shortCode);
				} else if (textColor == null) {
					throw new IllegalArgumentException("Cannot set just the background color.");
				} else {
					return String.format("%s%s,%s", COLOR_PREFIX, textColor.shortCode, backgroundColor.shortCode);
				}
			}
		}
	}

	@Nonnull
	public static String getColorPrefix(@Nullable IrcColor textColor, @Nullable IrcColor backgroundColor, @Nonnull String text) {
		return getColorPrefix(textColor, backgroundColor, text.length() > 0 && text.charAt(0) >= '0' && text.charAt(0) <= '9');
	}

	@Nonnull
	public static String getColoredText(@Nullable IrcColor textColor, @Nullable IrcColor backgroundColor, @Nonnull String text) {
		return String.format("%s%s", getColorPrefix(textColor, backgroundColor, text.length() > 0 && text.charAt(0) >= '0' && text.charAt(0) <= '9'), text);
	}
}