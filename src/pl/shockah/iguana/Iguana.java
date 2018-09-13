package pl.shockah.iguana;

import javax.annotation.Nonnull;

import lombok.Getter;

public class Iguana {
	@Getter
	private IguanaSession session;

	public static void main(String[] args) {
		//new Iguana().start();
	}

	public void start(@Nonnull Configuration config) throws IguanaSession.SessionException {
		session = new IguanaSession(config);
	}
}