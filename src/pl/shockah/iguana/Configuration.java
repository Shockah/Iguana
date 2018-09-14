package pl.shockah.iguana;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import pl.shockah.jay.JSONObject;

public final class Configuration {
	@Nonnull
	public static Configuration read(@Nonnull JSONObject json) {
		Configuration config = new Configuration();
		json.onObject("discord", jDiscord -> {
			jDiscord.onString("token", config.discord::setToken);
		});
		json.onObject("irc", jIrc -> {
			jIrc.onList("servers", jServers -> {
				for (JSONObject jServer : jServers.ofObjects()) {
					IRC.Server server = new IRC.Server();
					jServer.onString("host", server::setHost);
					jServer.onInt("port", server::setPort);
					jServer.onString("nickname", server::setNickname);
					jServer.onLong("discordChannelCategoryId", server::setDiscordChannelCategoryId);
					jServer.onLong("discordManagementChannelId", server::setDiscordManagementChannelId);
					jServer.onString("nickServLogin", server::setNickServLogin);
					jServer.onString("nickServPassword", server::setNickServPassword);
					jServer.onList("channels", jChannels -> {
						for (JSONObject jChannel : jChannels.ofObjects()) {
							IRC.Server.Channel channel = new IRC.Server.Channel();
							jChannel.onString("name", channel::setName);
							jChannel.onLong("discordChannelId", channel::setDiscordChannelId);
							jChannel.onLong("webhookId", channel::setWebhookId);
							server.channels.add(channel);
						}
					});
					config.irc.servers.add(server);
				}
			});
		});
		return config;
	}

	@Nonnull
	public final Discord discord = new Discord();

	@Getter
	@Setter
	public static final class Discord {
		private String token;
	}

	@Nonnull
	public final IRC irc = new IRC();

	@Getter
	@Setter
	public static final class IRC {
		private long discordManagementChannelId;

		@Nonnull
		private final List<Server> servers = new ArrayList<>();

		@Nonnull
		public TextChannel getDiscordManagementChannel(@Nonnull JDA jda) {
			return jda.getTextChannelById(discordManagementChannelId);
		}

		@Getter
		@Setter
		public static final class Server {
			private String host;

			private int port = 6667;

			private String nickname;

			private long discordChannelCategoryId;

			private long discordManagementChannelId;

			private String nickServLogin;

			private String nickServPassword;

			@Nonnull
			private final List<Channel> channels = new ArrayList<>();

			@Nonnull
			public Category getDiscordChannelCategory(@Nonnull JDA jda) {
				return jda.getCategoryById(discordChannelCategoryId);
			}

			@Nonnull
			public TextChannel getDiscordManagementChannel(@Nonnull JDA jda) {
				return jda.getTextChannelById(discordManagementChannelId);
			}

			@Getter
			@Setter
			public static final class Channel {
				private String name;

				private String password;

				private long discordChannelId;

				private long webhookId;

				@Nonnull
				public TextChannel getDiscordChannel(@Nonnull JDA jda) {
					return jda.getTextChannelById(discordChannelId);
				}

				@Nonnull
				public Webhook getWebhook(@Nonnull JDA jda) {
					return jda.getWebhookById(webhookId).complete();
				}
			}
		}
	}
}