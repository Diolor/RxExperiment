/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lorentzos.rxexperiment.continuous;

import java.io.Serializable;

import rx.Observer;

/**
 *
 */
public final class NotificationLite<T> {
	private NotificationLite() {
	}

	@SuppressWarnings("rawtypes")
	private static final NotificationLite INSTANCE = new NotificationLite();

	/**
	 * Gets the {@code NotificationLite} singleton.
	 *
	 * @return the sole {@code NotificationLite} object
	 */
	@SuppressWarnings("unchecked")
	public static <T> NotificationLite<T> instance() {
		return INSTANCE;
	}

	private static final Object ON_NEXT_NULL_SENTINEL = new Serializable() {
		private static final long serialVersionUID = 2;

		@Override
		public String toString() {
			return "Notification=>NULL";
		}
	};

	/**
	 * Creates a lite {@code onNext} notification for the value passed in without doing any allocation. Can
	 * be unwrapped and sent with the {@link #accept} method.
	 *
	 * @param t the item emitted to {@code onNext}
	 * @return the item, or a null token representing the item if the item is {@code null}
	 */
	public Object next(T t) {
		if (t == null)
			return ON_NEXT_NULL_SENTINEL;
		else
			return t;
	}

	/**
	 * Unwraps the lite notification and calls the appropriate method on the {@link Observer}.
	 *
	 * @param o the {@link Observer} to call {@code onNext}, {@code onCompleted}, or {@code onError}.
	 * @param n the lite notification
	 * @return {@code true} if {@code n} represents a termination event; {@code false} otherwise
	 * @throws IllegalArgumentException if the notification is null.
	 * @throws NullPointerException     if the {@link Observer} is null.
	 */
	@SuppressWarnings("unchecked")
	public boolean accept(ContinuousObserver<? super T> o, Object n) {
		if (n == ON_NEXT_NULL_SENTINEL) {
			o.onNext(null);
			return false;
		} else if (n != null) {
			o.onNext((T) n);
			return false;
		} else {
			throw new IllegalArgumentException("The lite notification can not be null");
		}
	}

	/**
	 * Indicates whether or not the lite notification represents a wrapped {@code null} {@code onNext} event.
	 *
	 * @param n the lite notification
	 * @return {@code true} if {@code n} represents a wrapped {@code null} {@code onNext} event, {@code false} otherwise
	 */
	public boolean isNull(Object n) {
		return n == ON_NEXT_NULL_SENTINEL;
	}

	/**
	 * Indicates whether or not the lite notification represents an {@code onNext} event.
	 *
	 * @param n the lite notification
	 * @return {@code true} if {@code n} represents an {@code onNext} event, {@code false} otherwise
	 */
	public boolean isNext(Object n) {
		return n != null;
	}

	/**
	 * Indicates which variety a particular lite notification is. If you need something more complex than
	 * simply calling the right method on an {@link Observer} then you can use this method to get the
	 * {@link Kind}.
	 *
	 * @param n the lite notification
	 * @return the {@link Kind} of lite notification {@code n} is: either {@code Kind.OnCompleted},
	 * {@code Kind.OnError}, or {@code Kind.OnNext}
	 * @throws IllegalArgumentException if the notification is null.
	 */
	public Kind kind(Object n) {
		if (n == null)
			throw new IllegalArgumentException("The lite notification can not be null");
		else
			// value or ON_NEXT_NULL_SENTINEL but either way it's an OnNext
			return Kind.OnNext;
	}

	/**
	 * Returns the item corresponding to this {@code OnNext} lite notification. Bad things happen if you pass
	 * this an {@code OnComplete} or {@code OnError} notification type. For performance reasons, this method
	 * does not check for this, so you are expected to prevent such a mishap.
	 *
	 * @param n the lite notification (of type {@code Kind.OnNext})
	 * @return the unwrapped value, which can be null
	 */
	@SuppressWarnings("unchecked")
	public T getValue(Object n) {
		return n == ON_NEXT_NULL_SENTINEL ? null : (T) n;
	}

	public enum Kind {
		OnNext
	}
}
