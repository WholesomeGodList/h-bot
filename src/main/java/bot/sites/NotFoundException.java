package bot.sites;

//simple wrapper for an exception when i don't find something
public class NotFoundException extends RuntimeException {
	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException() {
		super();
	}
}
