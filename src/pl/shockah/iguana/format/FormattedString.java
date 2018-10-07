package pl.shockah.iguana.format;

import javax.annotation.Nonnull;

public class FormattedString {
	public final boolean bold;

	public final boolean italic;

	public final boolean underline;

	public final boolean strikethrough;

	public final boolean inverse;

	@Nonnull
	public final IrcColor textColor;

	@Nonnull
	public final IrcColor backgroundColor;

	@Nonnull
	public final String text;

	public FormattedString(@Nonnull String text) {
		this(false, false, false, false, IrcColor.Default, IrcColor.Default, text);
	}

	public FormattedString(boolean bold, boolean italic, boolean underline, boolean strikethrough, @Nonnull IrcColor textColor, @Nonnull IrcColor backgroundColor, @Nonnull String text) {
		this(bold, italic, underline, strikethrough, false, textColor, backgroundColor, text);
	}

	public FormattedString(boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean inverse, @Nonnull IrcColor textColor, @Nonnull IrcColor backgroundColor, @Nonnull String text) {
		this.bold = bold;
		this.italic = italic;
		this.underline = underline;
		this.strikethrough = strikethrough;
		this.inverse = inverse;
		this.textColor = textColor;
		this.backgroundColor = backgroundColor;
		this.text = text;
	}
}