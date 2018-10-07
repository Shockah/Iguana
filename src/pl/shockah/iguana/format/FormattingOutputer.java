package pl.shockah.iguana.format;

import java.util.List;

import javax.annotation.Nonnull;

public interface FormattingOutputer<Context> {
	@Nonnull
	String output(@Nonnull List<FormattedString> formattedStrings, Context context);
}