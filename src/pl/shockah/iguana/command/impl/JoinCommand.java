package pl.shockah.iguana.command.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.bridge.IrcServerBridge;
import pl.shockah.iguana.command.ArgumentSet;
import pl.shockah.iguana.command.ArgumentSetParser;
import pl.shockah.iguana.command.ArgumentSetParserException;
import pl.shockah.iguana.command.NamedCommand;
import pl.shockah.iguana.command.ServerCommand;

public class JoinCommand implements ServerCommand, NamedCommand {
	@Nonnull
	@Getter
	private final String name = "join";

	@Override
	public void execute(@Nonnull IrcServerBridge server, @Nonnull Message executingMessage, @Nonnull String unparsedInput) {
		try {
			Input input = new ArgumentSetParser<>(Input.class).parse(unparsedInput);

			boolean alreadyKnown = server.getIrcServerConfig().getChannels().stream()
					.anyMatch(channelConfig -> channelConfig.getName().equalsIgnoreCase(input.channelName));
			if (alreadyKnown)
				return;

			Configuration.IRC.Server.Channel newChannelConfig = new Configuration.IRC.Server.Channel();
			newChannelConfig.setName(input.channelName);
			if (input.password != null)
				newChannelConfig.setPassword(input.password);

			server.getIrcServerConfig().getChannels().add(newChannelConfig);
			new Thread(() -> {
				server.getSession().getBridge().prepareIrcChannelTask(server.getIrcServerConfig(), newChannelConfig).run();
				server.getSession().saveConfiguration();
			}).start();
		} catch (ArgumentSetParserException e) {
			executingMessage.getTextChannel().sendMessage(new EmbedBuilder()
					.setTitle("Command parse error")
					.setDescription(e.getMessage())
					.build()).queue();
		}
	}

	private final class Input extends ArgumentSet {
		@Argument(isDefault = true)
		public String channelName;

		@Nullable
		@Argument(isRequired = false)
		public String password;
	}
}