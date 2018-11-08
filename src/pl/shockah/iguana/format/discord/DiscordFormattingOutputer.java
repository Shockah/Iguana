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

	private static final int OFFSET_X = 0;

	private static final int OFFSET_Y = -3;

	private static final int PADDING_X = 2;

	private static final int PADDING_Y = 2;

	private static final int MAX_LINE_LENGTH = 80;

	@Nonnull
	@Getter(lazy = true)
	private static final FontProvider fontProvider = ((Supplier<FontProvider>)() -> {
		try {
			List<FontProvider> providers = new ArrayList<>();
			GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();

			{
				Font font = Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/UbuntuMono-R.ttf"));
				environment.registerFont(font);
				environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/UbuntuMono-B.ttf")));
				environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/UbuntuMono-RI.ttf")));
				environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/UbuntuMono-BI.ttf")));
				providers.add(new BasicFontProvider(font.getFamily(), FONT_SIZE));
			}

			{
				Font font = Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/unifont-11.0.02.ttf"));
				environment.registerFont(font);
				providers.add(new BasicFontProvider(font.getFamily(), FONT_SIZE));
			}

			for (String fontFamilyName : environment.getAvailableFontFamilyNames()) {
				if (fontFamilyName.equals("Apple Color Emoji"))
					continue;
				providers.add(new BasicFontProvider(fontFamilyName, FONT_SIZE));
			}

			{
				Font font = Font.createFont(Font.TRUETYPE_FONT, DiscordFormattingOutputer.class.getResourceAsStream("/NotoEmoji-Regular.ttf"));
				environment.registerFont(font);
				providers.add(new BasicFontProvider(font.getFamily(), FONT_SIZE));
			}

			return new AlternativeFontProvider(providers);
		} catch (FontFormatException | IOException e) {
			throw new UnexpectedException(e);
		}
	}).get();

	private interface FontProvider {
		@Nonnull
		Font provide(boolean bold, boolean italic);

		@Nonnull
		Font provide(int codepoint, boolean bold, boolean italic);
	}

	private static class BasicFontProvider implements FontProvider {
		@Nonnull
		public final Font plainFont;

		@Nonnull
		public final Font boldFont;

		@Nonnull
		public final Font italicFont;

		@Nonnull
		public final Font boldItalicFont;

		private BasicFontProvider(@Nonnull String fontFamilyName, int size) {
			this(
					new Font(fontFamilyName, Font.PLAIN, size),
					new Font(fontFamilyName, Font.BOLD, size),
					new Font(fontFamilyName, Font.ITALIC, size),
					new Font(fontFamilyName, Font.BOLD | Font.ITALIC, size)
			);
		}

		private BasicFontProvider(@Nonnull Font plainFont, @Nonnull Font boldFont, @Nonnull Font italicFont, @Nonnull Font boldItalicFont) {
			this.plainFont = plainFont;
			this.boldFont = boldFont;
			this.italicFont = italicFont;
			this.boldItalicFont = boldItalicFont;
		}

		@Nonnull
		@Override
		public Font provide(boolean bold, boolean italic) {
			if (bold && italic)
				return boldItalicFont;
			else if (bold)
				return boldFont;
			else if (italic)
				return italicFont;
			else
				return plainFont;
		}

		@Nonnull
		@Override
		public Font provide(int codepoint, boolean bold, boolean italic) {
			return provide(bold, italic);
		}
	}

	private static class AlternativeFontProvider implements FontProvider {
		@Nonnull
		private final List<FontProvider> providers;

		private AlternativeFontProvider(@Nonnull List<FontProvider> providers) {
			if (providers.isEmpty())
				throw new IllegalArgumentException();
			this.providers = new ArrayList<>(providers);
		}

		@Nonnull
		@Override
		public Font provide(boolean bold, boolean italic) {
			return providers.get(0).provide(bold, italic);
		}

		@Nonnull
		@Override
		public Font provide(int codepoint, boolean bold, boolean italic) {
			for (FontProvider provider : providers) {
				Font font = provider.provide(codepoint, bold, italic);
				if (font.canDisplay(codepoint))
					return font;
			}
			return providers.get(0).provide(codepoint, bold, italic);
		}
	}

	@Getter(lazy = true)
	private static final int characterHeight = ((IntSupplier)() -> {
		Font font = getFontProvider().provide(false, false);
		FontRenderContext context = new FontRenderContext(font.getTransform(), true, true);
		return (int)Math.ceil(font.getStringBounds("Wy", context).getHeight());
	}).getAsInt();

	@Nonnull
	private final Configuration.Appearance appearanceConfiguration;

	public DiscordFormattingOutputer(@Nonnull Configuration.Appearance appearanceConfiguration) {
		this.appearanceConfiguration = appearanceConfiguration;
	}

	@Nonnull
	@Override
	public String escapeFormatting(@Nonnull String input) {
		return input.replaceAll("([*_~`\\\\])", "\\\\$1");
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
		boolean wasNewline = false;
		for (List<FormattedCharacter> wordOrWhitespace : wordsOrWhitespaces) {
			if (wordOrWhitespace.isEmpty())
				continue;
			if (wordOrWhitespace.get(0).codepoint == (int)'\n') {
				lines.add(current);
				current = new ArrayList<>();
				wasNewline = true;
				continue;
			}

			if (Character.isWhitespace(wordOrWhitespace.get(0).codepoint)) {
				lastWhitespaceGroup.addAll(wordOrWhitespace);
			} else {
				int newLength = current.size() + lastWhitespaceGroup.size() + wordOrWhitespace.size();
				if (newLength > MAX_LINE_LENGTH) {
					lines.add(current);
					current = new ArrayList<>();
				}

				if (wasNewline || !current.isEmpty())
					current.addAll(lastWhitespaceGroup);
				wasNewline = false;
				current.addAll(wordOrWhitespace);
				lastWhitespaceGroup = new ArrayList<>();
			}
		}

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

		BufferedImage image = new BufferedImage(totalWidth + PADDING_X * 2, characterHeight * lines.size() + PADDING_Y * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();

		graphics.setColor(appearanceConfiguration.getIrcDefaultBackgroundColor());
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		int lineIndex = 0;
		for (List<FormattedCharacter> line : lines) {
			int x = 0;
			for (FormattedCharacter character : line) {
				if (character.backgroundColor != IrcColor.Default) {
					Color color = appearanceConfiguration.getColor(character.backgroundColor);
					graphics.setColor(color);
					graphics.fillRect(x + PADDING_X, lineIndex * characterHeight + PADDING_Y, character.getWidth(), characterHeight);
				}

				Color color = appearanceConfiguration.getIrcDefaultTextColor();
				if (character.textColor != IrcColor.Default)
					color = appearanceConfiguration.getColor(character.textColor);
				graphics.setColor(color);
				graphics.setFont(character.getFont());
				graphics.drawString(character.getString(), x + PADDING_X + OFFSET_X, (lineIndex + 1) * characterHeight + PADDING_Y + OFFSET_Y);

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
		private final Font font = getFontProvider().provide(codepoint, bold, italic);

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