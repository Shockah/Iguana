package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import org.pircbotx.UserHostmask;

import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.command.CommandManager;
import pl.shockah.unicorn.color.LCHColorSpace;
import pl.shockah.util.Box;
import pl.shockah.util.ReadWriteMap;

public class IrcBridge extends ListenerAdapter {
	@Nonnull
	protected static final String ALLOWED_NONALPHANUMERIC_NICKNAME_CHARACTERS = "[\\]^_-{|}";

	@Nonnull
	protected static final float[] NICKNAME_LENGTH_LIGHTNESS = new float[] { 0.55f, 0.65f, 0.75f, 0.85f };

	@Nonnull
	protected static final float[] NICKNAME_LENGTH_CHROMA = new float[] { 0.4f, 0.5f, 0.6f, 0.7f, 0.8f };

	@Nonnull
	@Getter
	private final IguanaSession session;

	@Nonnull
	@Getter
	private final ReadWriteMap<Configuration.IRC.Server, IrcServerBridge> servers = new ReadWriteMap<>(new HashMap<>());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordManagementChannel = session.getConfiguration().irc.getDiscordManagementChannel(session.getDiscord());

	@Nonnull
	@Getter
	private final CommandManager commandManager = new CommandManager();

	public IrcBridge(@Nonnull IguanaSession session) {
		this.session = session;
	}

	private float getAtReversingRepeatingIndex(@Nonnull float[] array, int index) {
		int maxIndex = array.length * 2 - 2;
		int semiIndex = index % maxIndex;
		if (semiIndex < array.length)
			return array[semiIndex];
		else
			return array[maxIndex - semiIndex];
	}

	@Nonnull
	private LCHColorSpace getLchBackgroundColorForNickname(@Nonnull String nickname) {
		StringBuilder sb = new StringBuilder(nickname.toLowerCase());
		for (int i = 0; i < sb.length(); i++) {
			if (IrcBridge.ALLOWED_NONALPHANUMERIC_NICKNAME_CHARACTERS.indexOf(sb.charAt(i)) != -1)
				sb.deleteCharAt(i--);
		}
		for (int i = sb.length() - 1; i >= 0; i--) {
			char c = sb.charAt(i);
			if (c >= '0' && c <= '9')
				sb.deleteCharAt(i);
			else
				break;
		}

		return new LCHColorSpace(
				getAtReversingRepeatingIndex(IrcBridge.NICKNAME_LENGTH_LIGHTNESS, nickname.length() - 1) * 100f,
				getAtReversingRepeatingIndex(IrcBridge.NICKNAME_LENGTH_CHROMA, nickname.length() - 1) * 133f,
				(sb.toString().hashCode() % 100) / 100f
		);
	}

	@Nonnull
	private LCHColorSpace getLchTextColorForBackgroundColor(@Nonnull LCHColorSpace backgroundColor) {
		LCHColorSpace result = new LCHColorSpace(backgroundColor.l, backgroundColor.c, backgroundColor.h);
		if (backgroundColor.l < 50f)
			result.l += 40f;
		else
			result.l -= 40f;
		return result;
	}

	@Nullable
	public String getAvatarUrl(@Nonnull String nickname) {
		LCHColorSpace lchBackgroundColor = getLchBackgroundColorForNickname(nickname);
		LCHColorSpace lchTextColor = getLchTextColorForBackgroundColor(lchBackgroundColor);
		return session.getConfiguration().appearance.getAvatarUrl(nickname, lchBackgroundColor.toRGB(), lchTextColor.toRGB());
	}

	@Nonnull
	public String getDiscordFormattedFullUserInfo(@Nonnull org.pircbotx.User user) {
		return String.format("**%s** (%s)", user.getNick(), getDiscordFormattedHostmask(user));
	}

	@Nonnull
	public String getDiscordFormattedHostmask(@Nonnull UserHostmask hostmask) {
		String fullNick = hostmask.getNick();
		if (hostmask.getExtbanPrefix() != null && !hostmask.getExtbanPrefix().equals(""))
			fullNick = String.format("%s:%s", hostmask.getExtbanPrefix(), fullNick);
		return String.format("`%s!%s@%s`", fullNick, hostmask.getLogin(), hostmask.getHostname());
	}

	@Nonnull
	public Runnable prepareIrcTask() {
		return () -> {
			Guild guild;
			if (session.getConfiguration().discord.getGuildId() == 0) {
				guild = session.getDiscord().getGuilds().stream().findFirst().orElseThrow(IllegalStateException::new);
				session.getConfiguration().discord.setGuildId(guild.getIdLong());
			} else {
				guild = session.getConfiguration().discord.getGuild(session.getDiscord());
			}

			if (session.getConfiguration().discord.getOwnerUserId() == 0) {
				User ownerUser = guild.getOwner().getUser();
				session.getConfiguration().discord.setOwnerUserId(ownerUser.getIdLong());
			}

			if (session.getConfiguration().irc.getDiscordManagementChannelId() == 0) {
				TextChannel discordManagementChannel = (TextChannel)guild.getController().createTextChannel("iguana").complete();
				session.getConfiguration().irc.setDiscordManagementChannelId(discordManagementChannel.getIdLong());
			}
		};
	}

	@Nonnull
	public Runnable prepareIrcServerTask(@Nonnull Configuration.IRC.Server ircServerConfig) {
		return () -> {
			Guild guild = session.getConfiguration().discord.getGuild(session.getDiscord());

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
			Guild guild = session.getConfiguration().discord.getGuild(session.getDiscord());

			Category discordCategory;
			if (ircServerConfig.getDiscordChannelCategoryId() == 0) {
				discordCategory = (Category)guild.getController().createCategory(ircServerConfig.getHost()).complete();
				ircServerConfig.setDiscordChannelCategoryId(discordCategory.getIdLong());
			} else {
				discordCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());
			}

			if (ircChannelConfig.getDiscordEventChannelId() == 0) {
				TextChannel discordEventChannel = (TextChannel)discordCategory.createTextChannel(String.format("%s-events", ircChannelConfig.getName())).complete();
				ircChannelConfig.setDiscordEventChannelId(discordEventChannel.getIdLong());
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

	@Nonnull
	public Runnable prepareIrcPrivateChannelTask(@Nonnull Configuration.IRC.Server ircServerConfig, @Nonnull Configuration.IRC.Server.Private ircPrivateConfig) {
		return () -> {
			Guild guild = session.getConfiguration().discord.getGuild(session.getDiscord());

			Category discordCategory;
			if (ircServerConfig.getDiscordChannelCategoryId() == 0) {
				discordCategory = (Category)guild.getController().createCategory(ircServerConfig.getHost()).complete();
				ircServerConfig.setDiscordChannelCategoryId(discordCategory.getIdLong());
			} else {
				discordCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());
			}

			TextChannel discordChannel;
			if (ircPrivateConfig.getDiscordChannelId() == 0) {
				discordChannel = (TextChannel)discordCategory.createTextChannel(ircPrivateConfig.getName()).complete();
				ircPrivateConfig.setDiscordChannelId(discordChannel.getIdLong());
			} else {
				discordChannel = ircPrivateConfig.getDiscordChannel(session.getDiscord());
			}

			if (ircPrivateConfig.getWebhookId() == 0) {
				String webhookName = String.format("%s-@%s", ircServerConfig.getHost(), ircPrivateConfig.getName());
				if (webhookName.length() > 32)
					webhookName = webhookName.substring(0, 32);
				Webhook webhook = discordChannel.createWebhook(webhookName).complete();
				ircPrivateConfig.setWebhookId(webhook.getIdLong());
			}
		};
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.isWebhookMessage())
			return;

		User ownerUser = session.getConfiguration().discord.getOwnerUser(session.getDiscord());
		if (!ownerUser.equals(event.getMember().getUser()))
			return;

		servers.iterate((ircServerConfig, serverBridge, iterator) -> {
			Box<Boolean> handled = new Box<>(false);

			serverBridge.getChannels().iterate((ircChannelConfig, channelBridge, iterator2) -> {
				if (channelBridge.getDiscordChannel().equals(event.getChannel())) {
					channelBridge.onDiscordMessage(event);
					iterator.stop();
					iterator2.stop();
					handled.value = true;
				}
			});
		});
	}
}