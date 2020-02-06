package com.avenga.steamclient.exception;

public final class BerDecodeException extends RuntimeException {

	private static final int DEFAULT_POSITION = 0;

    private final int position;

	public BerDecodeException() {
		this.position = DEFAULT_POSITION;
	}

	public BerDecodeException(String message) {
		super(message);
		this.position = DEFAULT_POSITION;
	}

	public BerDecodeException(String message, Exception ex) {
		super(message, ex);
		this.position = DEFAULT_POSITION;
	}

	public BerDecodeException(String message, int position) {
		super(message);
		this.position = position;
	}

	public BerDecodeException(String message, int position, Exception ex) {
		super(message, ex);
		this.position = position;
	}

    public int getPosition() {
        return position;
    }

    @Override
	public String getMessage() {
        return super.getMessage() + String.format(" (Position %d)%s", position, System.lineSeparator());
	}
}