package pl.shockah.iguana;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Webhook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import pl.shockah.jay.JSONList;
import pl.shockah.jay.JSONObject;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.color.RGBColorSpace;

public final class Configuration {
	@Nonnull
	public static Configuration read(@Nonnull JSONObject json) {
		Configuration config = new Configuration();
		json.onObject("discord", jDiscord -> {
			jDiscord.onString("token", config.discord::setToken);
			jDiscord.onString("avatarUrlFormat", config.discord::setAvatarUrlFormat);
			jDiscord.onLong("guildId", config.discord::setGuildId);
			jDiscord.onLong("ownerUserId", config.discord::setOwnerUserId);
		});
		json.onObject("irc", jIrc -> {
			jIrc.onLong("discordManagementChannelId", config.irc::setDiscordManagementChannelId);
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
							jChannel.onLong("discordEventChannelId", channel::setDiscordEventChannelId);
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
	public JSONObject write() {
		return JSONObject.of(
				"discord", JSONObject.of(
						"token", discord.token,
						"avatarUrlFormat", discord.avatarUrlFormat,
						"guildId", discord.guildId,
						"ownerUserId", discord.ownerUserId
				),
				"irc", JSONObject.of(
						"discordManagementChannelId", irc.discordManagementChannelId,
						"servers", new JSONList<>(
								irc.servers.stream()
										.map(ircServerConfig -> JSONObject.of(
												"host", ircServerConfig.host,
												"port", ircServerConfig.port,
												"nickname", ircServerConfig.nickname,
												"discordChannelCategoryId", ircServerConfig.discordChannelCategoryId,
												"discordManagementChannelId", ircServerConfig.discordManagementChannelId,
												"nickServLogin", ircServerConfig.nickServLogin,
												"nickServPassword", ircServerConfig.nickServPassword,
												"channels", new JSONList<JSONObject>(
														ircServerConfig.channels.stream()
																.map(ircChannelConfig -> JSONObject.of(
																		"name", ircChannelConfig.name,
																		"discordChannelId", ircChannelConfig.discordChannelId,
																		"discordEventChannelId", ircChannelConfig.discordEventChannelId,
																		"webhookId", ircChannelConfig.webhookId
																))
																.collect(Collectors.toList())
												)
										))
										.collect(Collectors.toList())
						)
				)
		);
	}

	@Nonnull
	public final Discord discord = new Discord();

	@Getter
	@Setter
	public static final class Discord {
		private String token;

		private String avatarUrlFormat;

		private long guildId;

		private long ownerUserId;

		@Nullable
		public String getAvatarUrl(@Nonnull String nickname, @Nonnull RGBColorSpace backgroundColor, @Nonnull RGBColorSpace textColor) {
			if (avatarUrlFormat == null)
				return null;

			try {
				String url = avatarUrlFormat;
				url = url.replace("%nick%", URLEncoder.encode(nickname, "UTF-8"));
				url = url.replace("%backgroundRgb%", String.format("%02x%02x%02x", (int)(backgroundColor.r * 255), (int)(backgroundColor.g * 255), (int)(backgroundColor.b * 255)));
				url = url.replace("%textRgb%", String.format("%02x%02x%02x", (int)(textColor.r * 255), (int)(textColor.g * 255), (int)(textColor.b * 255)));
				return url;
			} catch (UnsupportedEncodingException e) {
				throw new UnexpectedException(e);
			}
		}

		@Nonnull
		public Guild getGuild(@Nonnull JDA jda) {
			return jda.getGuildById(guildId);
		}

		@Nonnull
		public User getOwnerUser(@Nonnull JDA jda) {
			return jda.getUserById(ownerUserId);
		}
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

				private long discordEventChannelId;

				private long webhookId;

				@Nonnull
				public TextChannel getDiscordChannel(@Nonnull JDA jda) {
					return jda.getTextChannelById(discordChannelId);
				}

				@Nonnull
				public TextChannel getDiscordEventChannel(@Nonnull JDA jda) {
					return jda.getTextChannelById(discordEventChannelId);
				}

				@Nonnull
				public Webhook getWebhook(@Nonnull JDA jda) {
					return jda.getWebhookById(webhookId).complete();
				}
			}
		}
	}
}