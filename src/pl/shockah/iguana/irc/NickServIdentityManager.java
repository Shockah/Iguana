package pl.shockah.iguana.irc;

import org.pircbotx.User;

import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import pl.shockah.unicorn.collection.ReadWriteMap;

public class NickServIdentityManager {
	@Nonnull
	private final ReadWriteMap<String, String> cache = new ReadWriteMap<>(new HashMap<>());

	@Nullable
	public String getAccountForUser(@Nonnull User user) {
		return getAccountForUser(user.getNick());
	}

	@Nullable
	public String getAccountForUser(@Nonnull String nick) {
		return cache.get(nick);
	}

	public void updateAccount(@Nonnull User user, @Nullable String account) {
		updateAccount(user.getNick(), account);
	}

	public void updateAccount(@Nonnull String nick, @Nullable String account) {
		if (account == null)
			cache.remove(nick);
		else
			cache.put(nick, account);
	}
}