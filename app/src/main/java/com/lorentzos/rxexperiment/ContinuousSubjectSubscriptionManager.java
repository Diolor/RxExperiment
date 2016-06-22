package com.lorentzos.rxexperiment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rx.functions.Action1;
import rx.functions.Actions;
import rx.subscriptions.Subscriptions;

/**
 * Represents the typical state and OnSubscribe logic for a Subject implementation.
 *
 * @param <T> the source and return value type
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class ContinuousSubjectSubscriptionManager<T> extends AtomicReference<ContinuousSubjectSubscriptionManager.State<T>> implements Continuous.OnSubscribe<T> {
	/**
	 * Stores the latest value or the terminal value for some Subjects.
	 */
	volatile Object latest;
	/**
	 * Indicates that the subject is active (cheaper than checking the state).
	 */
	boolean active = true;
	/**
	 * Action called when a new subscriber subscribes but before it is added to the state.
	 */
	Action1<ContinuousSubjectSubscriptionManager.SubjectObserver<T>> onStart = Actions.empty();
	/**
	 * Action called after the subscriber has been added to the state.
	 */
	Action1<ContinuousSubjectSubscriptionManager.SubjectObserver<T>> onAdded = Actions.empty();
	/**
	 * Action called when the subscriber wants to subscribe to a terminal state.
	 */
	Action1<ContinuousSubjectSubscriptionManager.SubjectObserver<T>> onTerminated = Actions.empty();
	/**
	 * The notification lite.
	 */
	public final NotificationLite<T> nl = NotificationLite.instance();

	public ContinuousSubjectSubscriptionManager() {
		super(ContinuousSubjectSubscriptionManager.State.EMPTY);
	}

	@Override
	public void call(ContinuousSubscriber<? super T> child) {
		ContinuousSubjectSubscriptionManager.SubjectObserver<T> bo = new SubjectObserver<T>(child);
		addUnsubscriber(child, bo);
		onStart.call(bo);
		if (!child.isUnsubscribed()) {
			if (add(bo) && child.isUnsubscribed()) {
				remove(bo);
			}
		}
	}

	/**
	 * Registers the unsubscribe action for the given subscriber.
	 */
	void addUnsubscriber(ContinuousSubscriber<? super T> child, final SubjectObserver<T> bo) {
		child.add(Subscriptions.create(() -> remove(bo)));
	}

	/**
	 * Set the latest NotificationLite value.
	 */
	void setLatest(Object value) {
		latest = value;
	}

	/**
	 * @return Retrieve the latest NotificationLite value
	 */
	Object getLatest() {
		return latest;
	}

	/**
	 * @return the array of active subscribers, don't write into the array!
	 */
	ContinuousSubjectSubscriptionManager.SubjectObserver<T>[] observers() {
		return get().observers;
	}

	/**
	 * Try to atomically add a SubjectObserver to the active state.
	 *
	 * @param o the SubjectObserver to add
	 * @return false if the subject is already in its terminal state
	 */
	boolean add(ContinuousSubjectSubscriptionManager.SubjectObserver<T> o) {
		do {
			ContinuousSubjectSubscriptionManager.State oldState = get();
			if (oldState.terminated) {
				onTerminated.call(o);
				return false;
			}
			ContinuousSubjectSubscriptionManager.State newState = oldState.add(o);
			if (compareAndSet(oldState, newState)) {
				onAdded.call(o);
				return true;
			}
		} while (true);
	}

	/**
	 * Atomically remove the specified SubjectObserver from the active observers.
	 *
	 * @param o the SubjectObserver to remove
	 */
	void remove(ContinuousSubjectSubscriptionManager.SubjectObserver<T> o) {
		do {
			ContinuousSubjectSubscriptionManager.State oldState = get();
			if (oldState.terminated) {
				return;
			}
			ContinuousSubjectSubscriptionManager.State newState = oldState.remove(o);
			if (newState == oldState || compareAndSet(oldState, newState)) {
				return;
			}
		} while (true);
	}

	/**
	 * Set a new latest NotificationLite value and return the active observers.
	 *
	 * @param n the new latest value
	 * @return the array of SubjectObservers, don't write into the array!
	 */
	ContinuousSubjectSubscriptionManager.SubjectObserver<T>[] next(Object n) {
		setLatest(n);
		return get().observers;
	}

	/**
	 * Atomically set the terminal NotificationLite value (which could be any of the 3),
	 * clear the active observers and return the last active observers.
	 *
	 * @param n the terminal value
	 * @return the last active SubjectObservers
	 */
	ContinuousSubjectSubscriptionManager.SubjectObserver<T>[] terminate(Object n) {
		setLatest(n);
		active = false;

		ContinuousSubjectSubscriptionManager.State<T> oldState = get();
		if (oldState.terminated) {
			return ContinuousSubjectSubscriptionManager.State.NO_OBSERVERS;
		}
		return getAndSet(ContinuousSubjectSubscriptionManager.State.TERMINATED).observers;
	}

	/**
	 * State-machine representing the termination state and active SubjectObservers.
	 */
	protected static final class State<T> {
		final boolean terminated;
		final ContinuousSubjectSubscriptionManager.SubjectObserver[] observers;
		static final ContinuousSubjectSubscriptionManager.SubjectObserver[] NO_OBSERVERS = new ContinuousSubjectSubscriptionManager.SubjectObserver[0];
		static final ContinuousSubjectSubscriptionManager.State TERMINATED = new ContinuousSubjectSubscriptionManager.State(true, NO_OBSERVERS);
		static final ContinuousSubjectSubscriptionManager.State EMPTY = new ContinuousSubjectSubscriptionManager.State(false, NO_OBSERVERS);

		public State(boolean terminated, ContinuousSubjectSubscriptionManager.SubjectObserver[] observers) {
			this.terminated = terminated;
			this.observers = observers;
		}

		public ContinuousSubjectSubscriptionManager.State add(ContinuousSubjectSubscriptionManager.SubjectObserver o) {
			ContinuousSubjectSubscriptionManager.SubjectObserver[] a = observers;
			int n = a.length;
			ContinuousSubjectSubscriptionManager.SubjectObserver[] b = new ContinuousSubjectSubscriptionManager.SubjectObserver[n + 1];
			System.arraycopy(observers, 0, b, 0, n);
			b[n] = o;
			return new ContinuousSubjectSubscriptionManager.State<T>(terminated, b);
		}

		public ContinuousSubjectSubscriptionManager.State remove(ContinuousSubjectSubscriptionManager.SubjectObserver o) {
			ContinuousSubjectSubscriptionManager.SubjectObserver[] a = observers;
			int n = a.length;
			if (n == 1 && a[0] == o) {
				return EMPTY;
			} else if (n == 0) {
				return this;
			}
			ContinuousSubjectSubscriptionManager.SubjectObserver[] b = new ContinuousSubjectSubscriptionManager.SubjectObserver[n - 1];
			int j = 0;
			for (int i = 0; i < n; i++) {
				ContinuousSubjectSubscriptionManager.SubjectObserver ai = a[i];
				if (ai != o) {
					if (j == n - 1) {
						return this;
					}
					b[j++] = ai;
				}
			}
			if (j == 0) {
				return EMPTY;
			}
			if (j < n - 1) {
				ContinuousSubjectSubscriptionManager.SubjectObserver[] c = new ContinuousSubjectSubscriptionManager.SubjectObserver[j];
				System.arraycopy(b, 0, c, 0, j);
				b = c;
			}
			return new ContinuousSubjectSubscriptionManager.State<T>(terminated, b);
		}
	}

	/**
	 * Observer wrapping the actual Subscriber and providing various
	 * emission facilities.
	 *
	 * @param <T> the consumed value type of the actual Observer
	 */
	protected static final class SubjectObserver<T> implements ContinuousObserver<T> {
		/**
		 * The actual Observer.
		 */
		final ContinuousSubscriber<? super T> actual;
		/**
		 * Was the emitFirst run? Guarded by this.
		 */
		boolean first = true;
		/**
		 * Guarded by this.
		 */
		boolean emitting;
		/**
		 * Guarded by this.
		 */
		List<Object> queue;
		/* volatile */ boolean fastPath;
		/**
		 * Indicate that the observer has caught up.
		 */
		protected volatile boolean caughtUp;
		/**
		 * Indicate where the observer is at replaying.
		 */
		private volatile Object index;

		public SubjectObserver(ContinuousSubscriber<? super T> actual) {
			this.actual = actual;
		}

		@Override
		public void onNext(T t) {
			actual.onNext(t);
		}

		/**
		 * Emits the given NotificationLite value and
		 * prevents the emitFirst to run if not already run.
		 *
		 * @param n  the NotificationLite value
		 * @param nl the type-appropriate notification lite object
		 */
		protected void emitNext(Object n, final NotificationLite<T> nl) {
			if (!fastPath) {
				synchronized (this) {
					first = false;
					if (emitting) {
						if (queue == null) {
							queue = new ArrayList<Object>();
						}
						queue.add(n);
						return;
					}
				}
				fastPath = true;
			}
			nl.accept(actual, n);
		}

		/**
		 * Tries to emit a NotificationLite value as the first
		 * value and drains the queue as long as possible.
		 *
		 * @param n  the NotificationLite value
		 * @param nl the type-appropriate notification lite object
		 */
		protected void emitFirst(Object n, final NotificationLite<T> nl) {
			synchronized (this) {
				if (!first || emitting) {
					return;
				}
				first = false;
				emitting = n != null;
			}
			if (n != null) {
				emitLoop(null, n, nl);
			}
		}

		/**
		 * Emits the contents of the queue as long as there are values.
		 *
		 * @param localQueue the initial queue contents
		 * @param current    the current content to emit
		 * @param nl         the type-appropriate notification lite object
		 */
		protected void emitLoop(List<Object> localQueue, Object current, final NotificationLite<T> nl) {
			boolean once = true;
			boolean skipFinal = false;
			try {
				do {
					if (localQueue != null) {
						for (Object n : localQueue) {
							accept(n, nl);
						}
					}
					if (once) {
						once = false;
						accept(current, nl);
					}
					synchronized (this) {
						localQueue = queue;
						queue = null;
						if (localQueue == null) {
							emitting = false;
							skipFinal = true;
							break;
						}
					}
				} while (true);
			} finally {
				if (!skipFinal) {
					synchronized (this) {
						emitting = false;
					}
				}
			}
		}

		/**
		 * Dispatches a NotificationLite value to the actual Observer.
		 *
		 * @param n  the value to dispatch
		 * @param nl the type-appropriate notification lite object
		 */
		protected void accept(Object n, final NotificationLite<T> nl) {
			if (n != null) {
				nl.accept(actual, n);
			}
		}

		/**
		 * @return the actual Observer.
		 */
		protected ContinuousObserver<? super T> getActual() {
			return actual;
		}

		/**
		 * Returns the stored index.
		 *
		 * @param <I> the index type
		 * @return the index value
		 */
		public <I> I index() {
			return (I) index;
		}

		/**
		 * Sets a new index value.
		 *
		 * @param newIndex the new index value
		 */
		public void index(Object newIndex) {
			this.index = newIndex;
		}
	}
}