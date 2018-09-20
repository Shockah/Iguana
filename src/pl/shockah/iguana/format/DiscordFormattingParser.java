package pl.shockah.iguana.format;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class DiscordFormattingParser implements FormattingParser {
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

		@Nonnull
		IrcColor textColor = IrcColor.Default;

		@Nonnull
		IrcColor backgroundColor = IrcColor.Default;

		private Processor(@Nonnull List<FormattedString> result) {
			this.result = result;
		}

		void process(int codePoint) {
			sb.appendCodePoint(codePoint);
		}

		private void push() {
			if (sb.length() != 0) {
				result.add(new FormattedString(bold, italic, underline, strikethrough, inverse, textColor, backgroundColor, sb.toString()));
				sb = new StringBuilder();
			}
		}
	}
}