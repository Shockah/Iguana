package pl.shockah.iguana.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

public class DiscordFormattingParser implements FormattingParser {
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
	@Override
	public List<FormattedString> parse(@Nonnull String message) {
		List<FormattedString> result = new ArrayList<>();
		Processor processor = new Processor(result);

		for (int i = 0; i < message.length(); ) {
			int codePoint = message.codePointAt(i);
			processor.process(codePoint);
			i += Character.charCount(codePoint);
		}

		processor.push();
		return result;
	}

	private static class Processor {
		@Nonnull
		final List<FormattedString> result;

		@Nonnull
		StringBuilder sb = new StringBuilder();

		boolean bold = false;

		boolean italic = false;

		boolean inverse = false;

		boolean underline = false;

		boolean strikethrough = false;

		private Processor(@Nonnull List<FormattedString> result) {
			this.result = result;
		}

		void process(int codePoint) {
			sb.appendCodePoint(codePoint);
		}

		private void push() {
			if (sb.length() != 0) {
				result.add(new FormattedString(bold, italic, underline, strikethrough, inverse, IrcColor.Default, IrcColor.Default, sb.toString()));
				sb = new StringBuilder();
			}
		}
	}
}