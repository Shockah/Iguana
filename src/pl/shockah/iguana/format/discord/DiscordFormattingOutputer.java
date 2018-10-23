package pl.shockah.iguana.format.discord;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Wither;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingOutputer;
import pl.shockah.iguana.format.IrcColor;
import pl.shockah.unicorn.collection.Either2;

public class DiscordFormattingOutputer implements FormattingOutputer<Void, Either2<String, Image>> {
	private static final int FONT_SIZE = 10;

	private static final int MAX_LINE_LENGTH = 50;

	private static final int CHARACTER_WIDTH = 10;

	private static final int CHARACTER_HEIGHT = 12;

	@Nonnull
	@Getter(lazy = true)
	private static final Font font = Font.loadFont(DiscordFormattingOutputer.class.getResource("unifont-11.0.02.ttf").toExternalForm(), FONT_SIZE);

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
		List<FormattedCharacter> characters = new ArrayList<>();
		for (FormattedString string : formattedStrings) {
			for (int i = 0; i < string.text.length(); i++) {
				characters.add(new FormattedCharacter(
						string.text.charAt(i),
						string.bold,
						string.italic,
						string.underline,
						string.strikethrough,
						string.textColor,
						string.backgroundColor
				));
			}
		}

		List<List<FormattedCharacter>> wordsOrWhitespaces = new ArrayList<>();
		List<FormattedCharacter> current = new ArrayList<>();
		Boolean whitespace = null;
		for (FormattedCharacter character : characters) {
			if (character.character == '\r')
				continue;

			if (character.character == '\n') {
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
				whitespace = Character.isWhitespace(character.character);
			if (!current.isEmpty() && whitespace != Character.isWhitespace(character.character)) {
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
			if (wordOrWhitespace.get(0).character == '\n') {
				lines.add(current);
				current = new ArrayList<>();
				continue;
			}

			if (Character.isWhitespace(wordOrWhitespace.get(0).character)) {
				lastWhitespaceGroup.addAll(wordOrWhitespace);
			} else {
				if (current.isEmpty()) {
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
			}
		}

		if (!lastWhitespaceGroup.isEmpty())
			current.addAll(lastWhitespaceGroup);
		if (!current.isEmpty())
			lines.add(current);

		return lines;
	}

	@Nonnull
	private Image createImage(@Nonnull List<List<FormattedCharacter>> lines) {
		int maxLineLength = lines.stream()
				.mapToInt(List::size)
				.max().orElse(0);

		if (maxLineLength == 0)
			throw new IllegalArgumentException("Empty image.");

		Canvas canvas = new Canvas(CHARACTER_WIDTH * maxLineLength, CHARACTER_HEIGHT * lines.size());
		GraphicsContext context = canvas.getGraphicsContext2D();
		context.setFont(getFont());

		context.setFill(new javafx.scene.paint.Color(1f, 1f, 1f, 0.5f));
		context.fill();

		int lineIndex = 0;
		for (List<FormattedCharacter> line : lines) {
			int inLineIndex = 0;
			for (FormattedCharacter character : line) {
				if (character.backgroundColor != IrcColor.Default) {
					Color color = appearanceConfiguration.getColor(character.backgroundColor);
					context.setFill(new javafx.scene.paint.Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f));
					context.fillRect(inLineIndex * CHARACTER_WIDTH, lineIndex * CHARACTER_HEIGHT, CHARACTER_WIDTH, CHARACTER_HEIGHT);
				}

				Color color = Color.BLACK;
				if (character.textColor != IrcColor.Default)
					appearanceConfiguration.getColor(character.textColor);
				context.setFill(new javafx.scene.paint.Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f));
				context.setFill(context.getStroke());
				context.fillText("" + character.character, inLineIndex * CHARACTER_WIDTH, lineIndex * CHARACTER_HEIGHT);

				inLineIndex++;
			}

			lineIndex++;
		}

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(javafx.scene.paint.Color.TRANSPARENT);
		return canvas.snapshot(params, null);
	}

	@EqualsAndHashCode
	private static class FormattedCharacter {
		final char character;

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

		private FormattedCharacter(char character) {
			this(character, false, false, false, false, IrcColor.Default, IrcColor.Default);
		}

		private FormattedCharacter(char character, boolean bold, boolean italic, boolean underline, boolean strikethrough, @Nonnull IrcColor textColor, @Nonnull IrcColor backgroundColor) {
			this.character = character;
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