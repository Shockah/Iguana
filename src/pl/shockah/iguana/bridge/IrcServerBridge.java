package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;

import org.pircbotx.PircBotX;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;

public class IrcServerBridge {
	@Nonnull
	@Getter
	private final IguanaSession session;

	@Nonnull
	@Getter
	private final PircBotX ircBot;

	@Nonnull
	@Getter
	private final Configuration.IRC.Server ircServerConfig;

	@Nonnull
	@Getter(lazy = true)
	private final Category discordChannelCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordManagementChannel = ircServerConfig.getDiscordManagementChannel(session.getDiscord());

	public IrcServerBridge(@Nonnull IguanaSession session, @Nonnull PircBotX ircBot, @Nonnull Configuration.IRC.Server ircServerConfig) {
		this.session = session;
		this.ircBot = ircBot;
		this.ircServerConfig = ircServerConfig;
	}
}