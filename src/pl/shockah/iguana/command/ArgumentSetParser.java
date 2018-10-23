package pl.shockah.iguana.command;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import pl.shockah.util.Box;

public class ArgumentSetParser<T extends ArgumentSet> {
	@Nonnull
	public static final String SPLIT_PATTERN = "(?m)(?<=\\s)(?!\\s)|(?<!\\s)(?=\\s+)";

	@Nonnull
	public static final Pattern ARGUMENT_NAME_PATTERN = Pattern.compile("-(\\S+)");

	@Nonnull
	public final Class<T> clazz;

	public ArgumentSetParser(@Nonnull Class<T> clazz) {
		this.clazz = clazz;
	}

	@Nonnull
	public T parse(@Nonnull String textInput) throws ArgumentSetParserException {
		try {
			T argumentSet = clazz.newInstance();
			ArgumentSetParseProcess process = new ArgumentSetParseProcess(argumentSet);

			String[] split = textInput.split(SPLIT_PATTERN);
			if (split.length != 0) {
				int offset = split[0].matches("(?m)\\s+") ? 1 : 0;
				for (int i = offset; i < split.length; i += 2) {
					Matcher m = ARGUMENT_NAME_PATTERN.matcher(split[i]);
					if (m.find()) {
						String argumentName = m.group(1);

						Box<Integer> index = new Box<>(i + 2);
						String rawValue = parseRawValue(split, index);
						i = index.value - 1;

						ArgumentSetParseProcess.Argument argument = process.nameToArgument.get(argumentName);
						if (argument == null)
							argumentSet.onUnknownArgument(argumentName, rawValue);
						else
							putArgumentValue(argument, rawValue);
						continue;
					}

					if (process.defaultArgument != null) {
						String rawValue = Arrays.stream(split).skip(i).collect(Collectors.joining(""));
						putArgumentValue(process.defaultArgument, rawValue);
						break;
					}
				}
			}

			for (ArgumentSetParseProcess.Argument argument : process.requiredArguments) {
				if (!process.alreadySetArguments.contains(argument))
					throw new ArgumentSetParserException(String.format("Missing required argument `%s`.", argument.name));
			}

			argumentSet.finalValidation();
			return argumentSet;
		} catch (Exception e) {
			throw new ArgumentSetParserException(e);
		}
	}

	@Nonnull
	protected String parseRawValue(@Nonnull String[] split, @Nonnull Box<Integer> index) {
		if (split[index.value].charAt(0) == '"') {
			StringBuilder sb = new StringBuilder();
			String[] copy = Arrays.copyOf(split, split.length);
			copy[index.value] = copy[index.value].substring(1);

			while (index.value < copy.length) {
				String s = copy[index.value];
				index.value++;
				if (s.endsWith("\"") && !s.endsWith("\\\"")) {
					sb.append(s, 0, s.length() - 1);
					return sb.toString();
				} else {
					if (ARGUMENT_NAME_PATTERN.matcher(s).find())
						return sb.toString();
					sb.append(s);
				}
			}

			return sb.toString();
		} else {
			return split[index.value++];
		}
	}

	public static boolean parseBoolean(@Nonnull String rawValue) throws ArgumentSetParserException {
		if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("t") || rawValue.equalsIgnoreCase("yes") || rawValue.equalsIgnoreCase("y"))
			return true;
		else if (rawValue.equalsIgnoreCase("false") || rawValue.equalsIgnoreCase("f") || rawValue.equalsIgnoreCase("no") || rawValue.equalsIgnoreCase("n"))
			return false;
		else
			throw new ArgumentSetParserException(String.format("Cannot parse `%s` as boolean.", rawValue));
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T extends Enum<?>> T parseEnum(@Nonnull Class<T> clazz, @Nonnull String rawValue) throws ArgumentSetParserException {
		for (Object obj : clazz.getEnumConstants()) {
			Enum<?> enumConst = (Enum<?>)obj;
			if (enumConst.name().equalsIgnoreCase(rawValue))
				return (T)enumConst;
		}

		try {
			int ordinal = Integer.parseInt(rawValue);
			for (Object obj : clazz.getEnumConstants()) {
				Enum<?> enumConst = (Enum<?>)obj;
				if (enumConst.ordinal() == ordinal)
					return (T)enumConst;
			}
		} catch (NumberFormatException ignored) {
		}

		throw new ArgumentSetParserException(String.format("Cannot parse `%s` as enum `%s`.", rawValue, clazz.getSimpleName()));
	}

	@SuppressWarnings("unchecked")
	protected void putArgumentValue(@Nonnull ArgumentSetParseProcess.Argument argument, @Nonnull String rawValue) throws ArgumentSetParserException {
		Class<?> clazz = argument.field.getType();
		switch (argument.type) {
			case Bool: {
				try {
					boolean value = parseBoolean(rawValue);
					if (clazz == boolean.class || clazz == Boolean.class) {
						putArgumentValueInternal(argument, value);
						return;
					}
				} catch (Exception e) {
					break;
				}
			} break;
			case Integer: {
				BigInteger value = new BigInteger(rawValue);
				if (clazz == BigInteger.class) {
					putArgumentValueInternal(argument, value);
					return;
				} else if (clazz == int.class) {
					putArgumentValueInternal(argument, value.intValueExact());
					return;
				} else if (clazz == long.class) {
					putArgumentValueInternal(argument, value.longValueExact());
					return;
				}
			} break;
			case Decimal: {
				BigDecimal value = new BigDecimal(rawValue);
				if (clazz == BigDecimal.class) {
					putArgumentValueInternal(argument, value);
					return;
				} else if (clazz == float.class) {
					putArgumentValueInternal(argument, value.floatValue());
					return;
				} else if (clazz == long.class) {
					putArgumentValueInternal(argument, value.doubleValue());
					return;
				}
			} break;
			case String: {
				if (clazz == String.class) {
					putArgumentValueInternal(argument, rawValue);
					return;
				}
			} break;
			case Enum: {
				try {
					putArgumentValueInternal(argument, parseEnum((Class<? extends Enum<?>>)clazz, rawValue));
					return;
				} catch (Exception e) {
					break;
				}
			}
			default:
				break;
		}
		throw new ArgumentSetParserException(String.format("Cannot handle argument `%s` of type `%s`.", argument.name, argument.type.name()));
	}

	private void putArgumentValueInternal(@Nonnull ArgumentSetParseProcess.Argument argument, @Nullable Object value) throws ArgumentSetParserException {
		if (!argument.argumentSet.isValueValid(argument.field, value))
			throw new ArgumentSetParserException(String.format("Invalid value for argument `%s`.", argument.name));
		argument.put(value);
	}
}