package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;

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
	public Runnable prepareIrcTask() {
		return () -> {
			Guild guild = session.getDiscord().getGuilds().stream().findFirst().orElseThrow(IllegalStateException::new);

			if (session.getConfiguration().irc.getDiscordManagementChannelId() == 0) {
				TextChannel discordManagementChannel = (TextChannel)guild.getController().createTextChannel("iguana").complete();
				session.getConfiguration().irc.setDiscordManagementChannelId(discordManagementChannel.getIdLong());
			}
		};
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
				prepareIrcChannelTask(ircServerConfig, ircChannelConfig).run();
			}
		};
	}

	@Nonnull
	public Runnable prepareIrcChannelTask(@Nonnull Configuration.IRC.Server ircServerConfig, @Nonnull Configuration.IRC.Server.Channel ircChannelConfig) {
		return () -> {
			Guild guild = session.getDiscord().getGuilds().stream().findFirst().orElseThrow(IllegalStateException::new);

			Category discordCategory;
			if (ircServerConfig.getDiscordChannelCategoryId() == 0) {
				discordCategory = (Category)guild.getController().createCategory(ircServerConfig.getHost()).complete();
				ircServerConfig.setDiscordChannelCategoryId(discordCategory.getIdLong());
			} else {
				discordCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());
			}

			TextChannel discordChannel;
			if (ircChannelConfig.getDiscordChannelId() == 0) {
				discordChannel = (TextChannel)discordCategory.createTextChannel(ircChannelConfig.getName()).complete();
				ircChannelConfig.setDiscordChannelId(discordChannel.getIdLong());
			} else {
				discordChannel = ircChannelConfig.getDiscordChannel(session.getDiscord());
			}

			if (ircChannelConfig.getWebhookId() == 0) {
				String webhookName = String.format("%s-%s", ircServerConfig.getHost(), ircChannelConfig.getName());
				if (webhookName.length() > 32)
					webhookName = webhookName.substring(0, 32);
				Webhook webhook = discordChannel.createWebhook(webhookName).complete();
				ircChannelConfig.setWebhookId(webhook.getIdLong());
			}
		};
	}
}