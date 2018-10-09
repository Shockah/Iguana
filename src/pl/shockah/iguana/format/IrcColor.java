package pl.shockah.iguana.format;

import javax.annotation.Nonnull;

import lombok.Getter;

public enum IrcColor {
	Default("99"),
	White("00"),
	Black("01"),
	Blue("02"),
	Green("03"),
	Red("04"),
	Maroon("05"),
	Purple("06"),
	Orange("07"),
	Yellow("08"),
	Lime("09"),
	Teal("10"),
	Cyan("11"),
	Royal("12"),
	Fuchsia("13"),
	Gray("14"),
	Silver("15");

	@Nonnull
	@Getter
	public final String shortCode;

	@Nonnull
	@Getter
	public final String code;

	IrcColor(@Nonnull String code) {
		shortCode = code.startsWith("0") ? code.substring(1) : code;
		this.code = code;
	}

	@Nonnull
	public static IrcColor fromCode(@Nonnull String code) {
		for (IrcColor color : values()) {
			if (color.shortCode.equals(code) || color.code.equals(code))
				return color;
		}
		throw new IllegalArgumentException(String.format("Invalid IRC color code `%s`.", code));
	}
}