package pl.shockah.iguana.irc;

import org.pircbotx.User;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import pl.shockah.unicorn.collection.ReadWriteMap;

public class NickServIdentityManager {
	@Nonnull
	private final ReadWriteMap<String, String> nickToAccount = new ReadWriteMap<>(new HashMap<>());

	@Nonnull
	private final ReadWriteMap<String, User> accountToUser = new ReadWriteMap<>(new HashMap<>());

	@Nullable
	public String getAccountForUser(@Nonnull User user) {
		return getAccountForUser(user.getNick());
	}

	@Nullable
	public String getAccountForUser(@Nonnull String nick) {
		return nickToAccount.get(nick);
	}

	@Nullable
	public User getUserForAccount(@Nonnull String account) {
		return accountToUser.get(account);
	}

	public void updateAccount(@Nonnull User user, @Nullable String account) {
		accountToUser.writeOperation(accountToUser -> {
			Iterator<Map.Entry<String, User>> it = accountToUser.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, User> entry = it.next();
				if (entry.getValue().equals(user)) {
					it.remove();
					break;
				}
			}

			updateAccount(user.getNick(), account);
			accountToUser.put(account, user);
		});
	}

	public void updateAccount(@Nonnull String nick, @Nullable String account) {
		if (account == null)
			nickToAccount.remove(nick);
		else
			nickToAccount.put(nick, account);
	}
}