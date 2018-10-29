package pl.shockah.iguana.command;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommandManager {
	@Nonnull
	private final Pattern commandPattern = Pattern.compile("^//?(\\S+)\\s+(.*)$");

	@Nonnull
	private final Set<GlobalCommand> globalCommands = new LinkedHashSet<>();

	@Nonnull
	private final Set<ServerCommand> serverCommands = new LinkedHashSet<>();

	@Nonnull
	private final Set<ChannelCommand> channelCommands = new LinkedHashSet<>();

	@Nonnull
	private final Set<UserCommand> userCommands = new LinkedHashSet<>();

	public <C extends GlobalCommand & NamedCommand> void register(@Nonnull C command) {
		globalCommands.add(command);
	}

	public <C extends ServerCommand & NamedCommand> void register(@Nonnull C command) {
		serverCommands.add(command);
	}

	public <C extends ChannelCommand & NamedCommand> void register(@Nonnull C command) {
		channelCommands.add(command);
	}

	public <C extends UserCommand & NamedCommand> void register(@Nonnull C command) {
		userCommands.add(command);
	}

	@Nullable
	public CommandCall parseCommandCall(@Nonnull String message) {
		Matcher m = commandPattern.matcher(message);
		if (m.find())
			return new CommandCall(m.group(1), m.group(2));
		return null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <C extends GlobalCommand & NamedCommand> C getCommandForGlobalContext(@Nonnull String name) {
		for (GlobalCommand command : globalCommands) {
			NamedCommand namedCommand = (NamedCommand)command;
			if (namedCommand.getName().equalsIgnoreCase(name))
				return (C)command;
		}
		return null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <C extends ServerCommand & NamedCommand> C getCommandForServerContext(@Nonnull String name) {
		for (ServerCommand command : serverCommands) {
			NamedCommand namedCommand = (NamedCommand)command;
			if (namedCommand.getName().equalsIgnoreCase(name))
				return (C)command;
		}

		GlobalCommand globalCommand = getCommandForGlobalContext(name);
		if (globalCommand != null)
			return (C)globalCommand.asServer();

		return null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <C extends ChannelCommand & NamedCommand> C getCommandForChannelContext(@Nonnull String name) {
		for (ChannelCommand command : channelCommands) {
			NamedCommand namedCommand = (NamedCommand)command;
			if (namedCommand.getName().equalsIgnoreCase(name))
				return (C)command;
		}

		ServerCommand serverCommand = getCommandForServerContext(name);
		if (serverCommand != null)
			return (C)serverCommand.asChannel();

		return null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <C extends UserCommand & NamedCommand> C getCommandForUserContext(@Nonnull String name) {
		for (UserCommand command : userCommands) {
			NamedCommand namedCommand = (NamedCommand)command;
			if (namedCommand.getName().equalsIgnoreCase(name))
				return (C)command;
		}

		ServerCommand serverCommand = getCommandForServerContext(name);
		if (serverCommand != null)
			return (C)serverCommand.asChannel();

		return null;
	}
}