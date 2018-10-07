package pl.shockah.iguana.format.irc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import pl.shockah.iguana.format.FormattedString;
import pl.shockah.iguana.format.FormattingParser;
import pl.shockah.iguana.format.IrcColor;

public class IrcFormattingParser implements FormattingParser<Void> {
	@Nonnull
	private static final Pattern colorPrefixPattern = Pattern.compile(
			String.format("%s%s", Pattern.quote(IrcFormattingConstants.COLOR_PREFIX), "((\\d\\d?)(?:,(\\d\\d?))?)?")
	);

	@Nonnull
	@Override
	public List<FormattedString> parse(@Nonnull String message, Void context) {
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

			if (handlePrefix(IrcFormattingConstants.RESET, prefix -> {
				bold = false;
				italic = false;
				inverse = false;
				underline = false;
				strikethrough = false;
				textColor = IrcColor.Default;
				backgroundColor = IrcColor.Default;
			}))
				return;

			if (handlePrefix(IrcFormattingConstants.BOLD, prefix -> bold = !bold))
				return;
			if (handlePrefix(IrcFormattingConstants.ITALIC, prefix -> italic = !italic))
				return;
			if (handlePrefix(IrcFormattingConstants.UNDERLINE, prefix -> underline = !underline))
				return;
			if (handlePrefix(IrcFormattingConstants.STRIKETHROUGH, prefix -> strikethrough = !strikethrough))
				return;
			if (handlePrefix(IrcFormattingConstants.INVERSE, prefix -> inverse = !inverse))
				return;

			Matcher colorMatcher = colorPrefixPattern.matcher(sb);
			if (colorMatcher.find()) {
				if ((colorMatcher.groupCount() == 3 && colorMatcher.group(3).length() == 2) || colorMatcher.end() != sb.length()) {
					String textColorCode = null;
					String backgroundColorCode = null;

					if (colorMatcher.groupCount() >= 2)
						textColorCode = colorMatcher.group(2);
					if (colorMatcher.groupCount() == 3)
						backgroundColorCode = colorMatcher.group(3);

					try {
						IrcColor newTextColor = null;
						IrcColor newBackgroundColor = null;

						if (textColorCode != null)
							newTextColor = IrcColor.fromCode(textColorCode);
						if (backgroundColorCode != null)
							newBackgroundColor = IrcColor.fromCode(backgroundColorCode);

						String stringToPush = sb.substring(0, colorMatcher.start());
						result.add(new FormattedString(bold, italic, underline, strikethrough, inverse, textColor, backgroundColor, stringToPush));
						sb.delete(0, stringToPush.length() + colorMatcher.group(0).length());

						if (newTextColor != null)
							textColor = newTextColor;
						if (newBackgroundColor != null)
							backgroundColor = newBackgroundColor;
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
		}

		private boolean handlePrefix(@Nonnull String prefix, @Nonnull PrefixHandler handler) {
			if (sb.lastIndexOf(prefix) != -1) {
				sb.delete(sb.length() - prefix.length(), sb.length());
				push();
				handler.handle(prefix);
				return true;
			} else {
				return false;
			}
		}

		private void push() {
			if (sb.length() != 0) {
				result.add(new FormattedString(bold, italic, underline, strikethrough, inverse, textColor, backgroundColor, sb.toString()));
				sb = new StringBuilder();
			}
		}

		interface PrefixHandler {
			void handle(@Nonnull String prefix);
		}
	}
}