package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.util.ReadWriteMap;

public class IrcBridge {
	@Nonnull
	@Getter
	private final IguanaSession session;

	@Nonnull
	@Getter
	private final ReadWriteMap<Configuration.IRC.Server, IrcServerBridge> servers = new ReadWriteMap<>(new HashMap<>());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordManagementChannel = session.getConfiguration().irc.getDiscordManagementChannel(session.getDiscord());

	public IrcBridge(@Nonnull IguanaSession session) {
		this.session = session;
	}

	@Nonnull
	public Runnable prepareIrcServerTask(@Nonnull Configuration.IRC.Server ircServerConfig) {
		return () -> {
			Guild guild = session.getDiscord().getGuilds().stream().findFirst().orElseThrow(IllegalStateException::new);

			Category discordCategory;
			if (ircServerConfig.getDiscordChannelCategoryId() == 0) {
				discordCategory = (Category)guild.getController().createCategory(ircServerConfig.getHost()).complete();
				ircServerConfig.setDiscordChannelCategoryId(discordCategory.getIdLong());
			} else {
				discordCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());
			}

			if (ircServerConfig.getDiscordManagementChannelId() == 0) {
				TextChannel discordManagementChannel = (TextChannel)discordCategory.createTextChannel("_iguana").complete();
				ircServerConfig.setDiscordManagementChannelId(discordManagementChannel.getIdLong());
			}

			for (Configuration.IRC.Server.Channel ircChannelConfig : ircServerConfig.getChannels()) {
				if (ircChannelConfig.getDiscordChannelId() == 0) {
					TextChannel channel = (TextChannel)discordCategory.createTextChannel(ircChannelConfig.getName()).complete();
					ircChannelConfig.setDiscordChannelId(channel.getIdLong());
				}
			}
		};
	}
}