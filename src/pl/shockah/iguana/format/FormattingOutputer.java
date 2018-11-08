package pl.shockah.iguana.format;

import java.util.List;

import javax.annotation.Nonnull;

public interface FormattingOutputer<Context, Output> {
	@Nonnull
	default String escapeFormatting(@Nonnull String input) {
		return input;
	}

	@Nonnull
	Output output(@Nonnull List<FormattedString> formattedStrings, Context context);
}