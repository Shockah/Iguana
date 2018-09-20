package pl.shockah.iguana.format;

import javax.annotation.Nonnull;

public class FormattedString {
	public final boolean bold;

	public final boolean italic;

	/**
	 * Either underline (Discord) or inverse (IRC).
	 */
	public final boolean emphasis;

	public final boolean strikethrough;

	@Nonnull
	public final IrcColor textColor;

	@Nonnull
	public final IrcColor backgroundColor;

	@Nonnull
	public final String text;

	public FormattedString(boolean bold, boolean italic, boolean emphasis, boolean strikethrough, @Nonnull IrcColor textColor, @Nonnull IrcColor backgroundColor, @Nonnull String text) {
		this.bold = bold;
		this.italic = italic;
		this.emphasis = emphasis;
		this.strikethrough = strikethrough;
		this.textColor = textColor;
		this.backgroundColor = backgroundColor;
		this.text = text;
	}
}