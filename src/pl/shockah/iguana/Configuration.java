package pl.shockah.iguana;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Webhook;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import pl.shockah.iguana.format.IrcColor;
import pl.shockah.jay.JSONList;
import pl.shockah.jay.JSONObject;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.color.RGBColorSpace;

public final class Configuration {
	@Nonnull
	private static Color readColor(@Nonnull JSONList<Number> jRgb) {
		if (jRgb.size() < 3 || jRgb.size() > 4)
			throw new IllegalArgumentException();

		if (jRgb.isInteger(0)) {
			int r = jRgb.getInt(0);
			int g = jRgb.getInt(1);
			int b = jRgb.getInt(2);
			int a = jRgb.size() == 4 ? jRgb.getInt(3) : 255;
			return new Color(r, g, b, a);
		} else if (jRgb.isDecimal(0)) {
			float r = jRgb.getFloat(0);
			float g = jRgb.getFloat(1);
			float b = jRgb.getFloat(2);
			float a = jRgb.size() == 4 ? jRgb.getFloat(3) : 1f;
			return new Color(r, g, b, a);
		}

		throw new IllegalArgumentException();
	}

	@Nonnull
	private static JSONList<? extends Number> writeColor(@Nonnull Color color) {
		JSONList<Number> result = new JSONList<>();
		result.add(color.getRed());
		result.add(color.getGreen());
		result.add(color.getBlue());
		if (color.getAlpha() < 255)
			result.add(color.getAlpha());
		return result;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static Configuration read(@Nonnull JSONObject json) {
		Configuration config = new Configuration();
		json.onObject("discord", jDiscord -> {
			jDiscord.onString("token", config.discord::setToken);
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
		json.onObject("appearance", jAppearance -> {
			jAppearance.onString("avatarUrlFormat", config.appearance::setAvatarUrlFormat);
			jAppearance.onList("defaultIrcBackgroundColor", jRawRgb -> config.appearance.setIrcDefaultBackgroundColor(readColor((JSONList<Number>)jRawRgb)));
			jAppearance.onList("defaultIrcTextColor", jRawRgb -> config.appearance.setIrcDefaultTextColor(readColor((JSONList<Number>)jRawRgb)));
			jAppearance.onObject("ircColors", jIrcColors -> {
				for (String jIrcColorKey : jIrcColors.keySet()) {
					try {
						IrcColor ircColor = IrcColor.valueOf(jIrcColorKey);
						config.appearance.ircColors.put(ircColor, readColor((JSONList<Number>)jIrcColors.getList(jIrcColorKey)));
					} catch (Exception ignored) {
					}
				}
			});
			jAppearance.onObject("events", jEvents -> {
				jEvents.onList("connectedColor", jRawRgb -> config.appearance.events.setConnectedColor(readColor((JSONList<Number>)jRawRgb)));
				jEvents.onList("disconnectedColor", jRawRgb -> config.appearance.events.setDisconnectedColor(readColor((JSONList<Number>)jRawRgb)));
			});
			jAppearance.onObject("userInfo", jUserInfo -> {
				jUserInfo.onList("joinColor", jRawRgb -> config.appearance.userInfo.setJoinColor(readColor((JSONList<Number>)jRawRgb)));
				jUserInfo.onList("leaveColor", jRawRgb -> config.appearance.userInfo.setLeaveColor(readColor((JSONList<Number>)jRawRgb)));
				jUserInfo.onList("nickChangeColor", jRawRgb -> config.appearance.userInfo.setNickChangeColor(readColor((JSONList<Number>)jRawRgb)));
			});
		});
		return config;
	}

	@Nonnull
	public JSONObject write() {
		return JSONObject.of(
				"discord", JSONObject.of(
						"token", discord.token,
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
				),
				"appearance", JSONObject.of(
						"avatarUrlFormat", appearance.avatarUrlFormat,
						"defaultIrcBackgroundColor", writeColor(appearance.ircDefaultBackgroundColor),
						"defaultIrcTextColor", writeColor(appearance.ircDefaultTextColor),
						"ircColors", new JSONObject(
								appearance.ircColors.entrySet().stream()
										.collect(Collectors.toMap(
												entry -> entry.getKey().name(),
												entry -> writeColor(entry.getValue())
										))
						),
						"events", JSONObject.of(
								"connectedColor", writeColor(appearance.events.connectedColor),
								"disconnectedColor", writeColor(appearance.events.disconnectedColor)
						),
						"userInfo", JSONObject.of(
								"joinColor", writeColor(appearance.userInfo.joinColor),
								"leaveColor", writeColor(appearance.userInfo.leaveColor),
								"nickChangeColor", writeColor(appearance.userInfo.nickChangeColor)
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

		private long guildId;

		private long ownerUserId;

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

			@Getter
			@Setter
			public static final class Private {
				private String name;

				private boolean isNickServAccount;

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

	@Nonnull
	public final Appearance appearance = new Appearance();

	@Getter
	@Setter
	public static final class Appearance {
		private String avatarUrlFormat;

		private Color ircDefaultBackgroundColor = Color.WHITE;

		private Color ircDefaultTextColor = Color.BLACK;

		@Nonnull
		private final Map<IrcColor, Color> ircColors = new TreeMap<>();

		@Nonnull
		public final Events events = new Events();

		@Nonnull
		public final UserInfo userInfo = new UserInfo();

		public Appearance() {
			ircColors.put(IrcColor.White, new Color(255, 255, 255));
			ircColors.put(IrcColor.Black, new Color(0, 0, 0));
			ircColors.put(IrcColor.Blue, new Color(0, 0, 126));
			ircColors.put(IrcColor.Green, new Color(0, 147, 0));
			ircColors.put(IrcColor.Red, new Color(254, 0, 0));
			ircColors.put(IrcColor.Maroon, new Color(126, 0, 0));
			ircColors.put(IrcColor.Purple, new Color(152, 0, 158));
			ircColors.put(IrcColor.Orange, new Color(253, 127, 0));
			ircColors.put(IrcColor.Yellow, new Color(255, 255, 0));
			ircColors.put(IrcColor.Lime, new Color(0, 252, 0));
			ircColors.put(IrcColor.Teal, new Color(0, 147, 147));
			ircColors.put(IrcColor.Cyan, new Color(0, 255, 253));
			ircColors.put(IrcColor.Royal, new Color(0, 0, 252));
			ircColors.put(IrcColor.Fuchsia, new Color(255, 0, 254));
			ircColors.put(IrcColor.Gray, new Color(89, 128, 127));
			ircColors.put(IrcColor.Silver, new Color(210, 210, 210));
		}

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
		public Color getColor(@Nonnull IrcColor ircColor) {
			return ircColors.get(ircColor);
		}

		@Getter
		@Setter
		public static final class Events {
			@Nonnull
			private Color connectedColor = new Color(30, 200, 30);

			@Nonnull
			private Color disconnectedColor = new Color(200, 30, 30);
		}

		@Getter
		@Setter
		public static final class UserInfo {
			@Nonnull
			private Color joinColor = new Color(30, 200, 30);

			@Nonnull
			private Color leaveColor = new Color(200, 30, 30);

			@Nonnull
			private Color nickChangeColor = new Color(140, 163, 255);
		}
	}
}