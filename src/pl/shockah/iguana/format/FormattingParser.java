package pl.shockah.iguana.format;

import java.util.List;

import javax.annotation.Nonnull;

public interface FormattingParser {
	@Nonnull
	List<FormattedString> parse(@Nonnull String message);
}