package pl.shockah.iguana.format.discord;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Wither;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.IrcColor;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.collection.Either2;

public class DiscordFormattingOutputer implements FormattingOutputer<Void, Either2<String, BufferedImage>> {
	private static final int FONT_SIZE = 14;

	private static final int MAX_LINE_LENGTH = 100;

	@Nonnull
	@Getter(lazy = true)
	private static final String fontFamilyName = ((Supplier<String>)() -> {
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/unifont-11.0.02.ttf"));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
			return font.getFamily();
		} catch (FontFormatException | IOException e) {
			throw new UnexpectedException(e);
		}
	}).get();

	@Nonnull
	@Getter(lazy = true)
	private static final Font plainFont = new Font(getFontFamilyName(), Font.PLAIN, FONT_SIZE);

	@Nonnull
	@Getter(lazy = true)
	private static final Font boldFont = new Font(getFontFamilyName(), Font.BOLD, FONT_SIZE);

	@Nonnull
	@Getter(lazy = true)
	private static final Font italicFont = new Font(getFontFamilyName(), Font.ITALIC, FONT_SIZE);

	@Nonnull
	@Getter(lazy = true)
	private static final Font boldItalicFont = new Font(getFontFamilyName(), Font.BOLD | Font.ITALIC, FONT_SIZE);

	@Getter(lazy = true)
	private static final int characterHeight = ((IntSupplier)() -> {
		Font font = getPlainFont();
		FontRenderContext context = new FontRenderContext(font.getTransform(), true, true);
		return (int)Math.ceil(font.getStringBounds("Wy", context).getHeight());
	}).getAsInt();

	@Nonnull
	private final Configuration.Appearance appearanceConfiguration;

	public DiscordFormattingOutputer(@Nonnull Configuration.Appearance appearanceConfiguration) {
		this.appearanceConfiguration = appearanceConfiguration;
	}

	@Nonnull
	private String escapeFormatting(@Nonnull String text) {
		return text.replaceAll("([*_~`\\\\])", "\\\\$1");
	}

	@Nonnull
	@Override
	public Either2<String, BufferedImage> output(@Nonnull List<FormattedString> formattedStrings, Void context) {
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
	private BufferedImage outputImage(@Nonnull List<FormattedString> formattedStrings, Void context) {
		List<FormattedCharacter> characters = new ArrayList<>();
		for (FormattedString string : formattedStrings) {
			for (int offset = 0; offset < string.text.length(); ) {
				int codepoint = string.text.codePointAt(offset);
				characters.add(new FormattedCharacter(
						codepoint,
						string.bold,
						string.italic,
						string.underline,
						string.strikethrough,
						string.textColor,
						string.backgroundColor
				));
				offset += Character.charCount(codepoint);
			}
		}

		List<List<FormattedCharacter>> wordsOrWhitespaces = new ArrayList<>();
		List<FormattedCharacter> current = new ArrayList<>();
		Boolean whitespace = null;
		for (FormattedCharacter character : characters) {
			if (character.codepoint == (int)'\r')
				continue;

			if (character.codepoint == (int)'\n') {
				if (!current.isEmpty()) {
					wordsOrWhitespaces.add(current);
					current = new ArrayList<>();
				}
				current.add(character);
				wordsOrWhitespaces.add(current);
				current = new ArrayList<>();
				continue;
			}

			if (whitespace == null)
				whitespace = Character.isWhitespace(character.codepoint);
			if (!current.isEmpty() && whitespace != Character.isWhitespace(character.codepoint)) {
				wordsOrWhitespaces.add(current);
				current = new ArrayList<>();
				whitespace = !whitespace;
			}
			current.add(character);
		}

		if (!current.isEmpty() && whitespace != null)
			wordsOrWhitespaces.add(current);

		return createImage(prepareLinesForImageOutput(wordsOrWhitespaces));
	}

	@Nonnull
	private List<List<FormattedCharacter>> prepareLinesForImageOutput(@Nonnull List<List<FormattedCharacter>> wordsOrWhitespaces) {
		List<List<FormattedCharacter>> lines = new ArrayList<>();

		List<FormattedCharacter> current = new ArrayList<>();
		List<FormattedCharacter> lastWhitespaceGroup = new ArrayList<>();
		for (List<FormattedCharacter> wordOrWhitespace : wordsOrWhitespaces) {
			if (wordOrWhitespace.isEmpty())
				continue;
			if (wordOrWhitespace.get(0).codepoint == (int)'\n') {
				lines.add(current);
				current = new ArrayList<>();
				continue;
			}

			if (Character.isWhitespace(wordOrWhitespace.get(0).codepoint)) {
				lastWhitespaceGroup.addAll(wordOrWhitespace);
			} else {
				if (current.isEmpty()) {
					if (lastWhitespaceGroup.size() > 1)
						current.addAll(lastWhitespaceGroup);
					lastWhitespaceGroup = new ArrayList<>();
				}

				int newLength = current.size() + lastWhitespaceGroup.size() + wordOrWhitespace.size();
				if (newLength > MAX_LINE_LENGTH) {
					lines.add(current);
					current = new ArrayList<>();
				}

				current.addAll(lastWhitespaceGroup);
				current.addAll(wordOrWhitespace);
				lastWhitespaceGroup = new ArrayList<>();
			}
		}

		if (!lastWhitespaceGroup.isEmpty())
			current.addAll(lastWhitespaceGroup);
		if (!current.isEmpty())
			lines.add(current);

		return lines;
	}

	@Nonnull
	private BufferedImage createImage(@Nonnull List<List<FormattedCharacter>> lines) {
		int totalWidth = lines.stream()
				.mapToInt(characters -> characters.stream()
						.mapToInt(FormattedCharacter::getWidth)
						.sum())
				.max()
				.orElseThrow(() -> new IllegalArgumentException("Empty image."));
		int characterHeight = getCharacterHeight();

		BufferedImage image = new BufferedImage(totalWidth, characterHeight * lines.size(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		int lineIndex = 0;
		for (List<FormattedCharacter> line : lines) {
			int x = 0;
			for (FormattedCharacter character : line) {
				if (character.backgroundColor != IrcColor.Default) {
					Color color = appearanceConfiguration.getColor(character.backgroundColor);
					graphics.setColor(color);
					graphics.fillRect(x, lineIndex * characterHeight, character.getWidth(), characterHeight);
				}

				Color color = Color.BLACK;
				if (character.textColor != IrcColor.Default)
					color = appearanceConfiguration.getColor(character.textColor);
				graphics.setColor(color);
				graphics.setFont(character.getFont());
				graphics.drawString(character.getString(), x, (lineIndex + 1) * characterHeight - 2);

				x += character.getWidth();
			}
			lineIndex++;
		}

		return image;
	}

	@EqualsAndHashCode
	private static class FormattedCharacter {
		final int codepoint;

		@Wither
		final boolean bold;

		@Wither
		final boolean italic;

		@Wither
		final boolean underline;

		@Wither
		final boolean strikethrough;

		@Nonnull
		public final IrcColor textColor;

		@Nonnull
		public final IrcColor backgroundColor;

		@Nonnull
		@Getter(lazy = true)
		private final String string = new String(Character.toChars(codepoint));

		@Nonnull
		@Getter(lazy = true)
		private final Font font = ((Supplier<Font>)() -> {
			if (bold && italic)
				return getBoldItalicFont();
			else if (bold)
				return getBoldFont();
			else if (italic)
				return getItalicFont();
			else
				return getPlainFont();
		}).get();

		@Getter(lazy = true)
		private final int width = ((IntSupplier)() -> {
			Font font = getFont();
			FontRenderContext context = new FontRenderContext(font.getTransform(), true, true);
			return (int)Math.ceil(font.getStringBounds(getString(), context).getWidth());
		}).getAsInt();

		private FormattedCharacter(int codepoint, boolean bold, boolean italic, boolean underline, boolean strikethrough, @Nonnull IrcColor textColor, @Nonnull IrcColor backgroundColor) {
			this.codepoint = codepoint;
			this.bold = bold;
			this.italic = italic;
			this.underline = underline;
			this.strikethrough = strikethrough;
			this.textColor = textColor;
			this.backgroundColor = backgroundColor;
		}

		public boolean sameFormatting(@Nonnull FormattedCharacter other) {
			return other.bold == bold && other.italic == italic && other.underline == underline && other.strikethrough == strikethrough
					&& other.textColor == textColor && other.backgroundColor == backgroundColor;
		}
	}
}