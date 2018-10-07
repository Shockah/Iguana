package pl.shockah.iguana.format.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingParser;
import pl.shockah.iguana.format.IrcColor;

public class DiscordFormattingParser implements FormattingParser<Void> {
	@Nonnull
	private static final Pattern tripleStarPattern = Pattern.compile("(?<!\\*)\\*{3}(?![\\s*])(.*?)(?<![\\s*])\\*{3}(?!\\*)");

	@Nonnull
	private static final Pattern doubleStarPattern = Pattern.compile("(?<!\\*)\\*{2}(?![\\s*])(.*?)(?<![\\s*])\\*{2}(?!\\*)");

	@Nonnull
	private static final Pattern singleStarPattern = Pattern.compile("(?<!\\*)\\*(?![\\s*])(.*?)(?<![\\s*])\\*(?!\\*)");

	@Nonnull
	private static final Pattern doubleUnderlinePattern = Pattern.compile("(?<!_)_{2}(?![\\s_])(.*?)(?<![\\s_])_{2}(?!_)");

	@Nonnull
	private static final Pattern singleUnderlinePattern = Pattern.compile("(?<!_)_(?![\\s_])(.*?)(?<![\\s_])_(?!_)");

	@Nonnull
	private static final Pattern strikethroughPattern = Pattern.compile("(?<!~)~{2}(?![\\s~])(.*?)(?<![\\s~])~{2}(?!~)");

	@Nonnull
	private static final Pattern inlineCodePattern = Pattern.compile("(?<!`)`(?![\\s`])(.*?)(?<![\\s`])`(?!`)");

	@Nonnull
	private static final Pattern codeBlockPattern = Pattern.compile("(?<!`)`{3}((?:\\S+)?)\\r?\\n\\s*((?=\\S).+?)\\r?\\n`{3}(?!`)");

	@Nonnull
	private static final Pattern backslashPattern = Pattern.compile("\\\\([*_~`])");

	@Nonnull
	@Override
	public List<FormattedString> parse(@Nonnull String message, Void context) {
		List<FormattedCharacter> characterFormats = new ArrayList<>();

		for (int i = 0; i < message.length(); i++) {
			characterFormats.add(new FormattedCharacter(message.charAt(i)));
		}

		handlePattern(codeBlockPattern, 2, message, characterFormats, input -> input.withIgnoreFurtherFormatting(true));
		handlePattern(inlineCodePattern, 1, message, characterFormats, input -> input);
		handlePattern(tripleStarPattern, 1, message, characterFormats, input -> input.withBold(true).withItalic(true));
		handlePattern(doubleStarPattern, 1, message, characterFormats, input -> input.withBold(true));
		handlePattern(singleStarPattern, 1, message, characterFormats, input -> input.withItalic(true));
		handlePattern(doubleUnderlinePattern, 1, message, characterFormats, input -> input.withUnderline(true));
		handlePattern(singleUnderlinePattern, 1, message, characterFormats, input -> input.withItalic(true));
		handlePattern(strikethroughPattern, 1, message, characterFormats, input -> input.withStrikethrough(true));
		handlePattern(backslashPattern, 1, message, characterFormats, input -> input);

		List<FormattedString> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		FormattedCharacter lastCharacter = null;

		for (FormattedCharacter character : characterFormats) {
			if (lastCharacter != null && !lastCharacter.sameFormatting(character)) {
				result.add(new FormattedString(
						lastCharacter.bold,
						lastCharacter.italic,
						lastCharacter.underline,
						lastCharacter.strikethrough,
						IrcColor.Default,
						IrcColor.Default,
						sb.toString()
				));
				sb = new StringBuilder();
			}
			sb.append(character.character);
			lastCharacter = character;
		}

		if (lastCharacter != null) {
			result.add(new FormattedString(
					lastCharacter.bold,
					lastCharacter.italic,
					lastCharacter.underline,
					lastCharacter.strikethrough,
					IrcColor.Default,
					IrcColor.Default,
					sb.toString()
			));
		}

		return result;
	}

	private void handlePattern(@Nonnull Pattern pattern, int contentGroup, @Nonnull String message, @Nonnull List<FormattedCharacter> characterFormats, @Nonnull PatternHandler handler) {
		Matcher m;

		while (true) {
			m = pattern.matcher(message);
			if (!m.find())
				break;

			int contentStart = m.start(contentGroup);
			int contentEnd = m.end(contentGroup);
			int matchStart = m.start();
			int matchEnd = m.end();

			for (int i = contentStart; i < contentEnd; i++) {
				FormattedCharacter formatted = characterFormats.get(i);
				if (!formatted.ignoreFurtherFormatting)
					characterFormats.set(i, handler.handle(formatted));
			}

			for (int i = contentEnd; i < matchEnd; i++) {
				characterFormats.remove(i);
			}

			for (int i = matchStart; i < contentStart; i++) {
				characterFormats.remove(i);
			}
		}
	}

	private interface PatternHandler {
		@Nonnull
		FormattedCharacter handle(@Nonnull FormattedCharacter input);
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

		@Wither
		final boolean ignoreFurtherFormatting;

		private FormattedCharacter(char character) {
			this(character, false, false, false, false, false);
		}

		private FormattedCharacter(char character, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean ignoreFurtherFormatting) {
			this.character = character;
			this.bold = bold;
			this.italic = italic;
			this.underline = underline;
			this.strikethrough = strikethrough;
			this.ignoreFurtherFormatting = ignoreFurtherFormatting;
		}

		public boolean sameFormatting(@Nonnull FormattedCharacter other) {
			return other.bold == bold && other.italic == italic && other.underline == underline && other.strikethrough == strikethrough;
		}
	}
}