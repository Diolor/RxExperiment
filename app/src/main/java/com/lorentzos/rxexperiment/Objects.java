package com.lorentzos.rxexperiment;

/**
 *
 */
enum Objects {
	;

	/**
	 * Verifies if the object is not null and returns it or throws a NullPointerException
	 * with the given message.
	 *
	 * @param <T>     the value type
	 * @param object  the object to verify
	 * @param message the message to use with the NullPointerException
	 * @return the object itself
	 * @throws NullPointerException if object is null
	 */
	public static <T> T requireNonNull(T object, String message) {
		if (object == null) {
			throw new NullPointerException(message);
		}
		return object;
	}

}
