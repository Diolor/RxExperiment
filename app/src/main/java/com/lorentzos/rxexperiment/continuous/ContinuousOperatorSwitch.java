package com.lorentzos.rxexperiment.continuous;

import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscription;
import rx.internal.operators.BackpressureUtils;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.atomic.SpscLinkedArrayQueue;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

/**
 *
 */
public class ContinuousOperatorSwitch<Downstream> implements ContinuousOperator<Downstream, Continuous<? extends Downstream>> {

	/** Lazy initialization via inner-class holder. */
	private static final class Holder {
		/** A singleton instance. */
		static final ContinuousOperatorSwitch<Object> INSTANCE = new ContinuousOperatorSwitch<>();
	}

	/**
	 * Returns a singleton instance of the operator based on the delayError parameter.
	 *
	 * @param <T> the value type
	 * @return a singleton instance of this stateless operator.
	 */
	@SuppressWarnings("unchecked")
	public static <T> ContinuousOperatorSwitch<T> instance() {
		return (ContinuousOperatorSwitch<T>) ContinuousOperatorSwitch.Holder.INSTANCE;
	}

	ContinuousOperatorSwitch() {
	}

	@Override
	public ContinuousSubscriber<? super Continuous<? extends Downstream>> call(ContinuousSubscriber<? super Downstream> child) {
		ContinuousOperatorSwitch.SwitchSubscriber<Downstream> sws = new ContinuousOperatorSwitch.SwitchSubscriber<>(child);
		child.add(sws);
		sws.init();
		return sws;
	}

	private static final class SwitchSubscriber<T> extends ContinuousSubscriber<Continuous<? extends T>> {
		final ContinuousSubscriber<? super T> child;
		final SerialSubscription ssub;
		final AtomicLong index;
		final SpscLinkedArrayQueue<Object> queue;
		final NotificationLite<T> nl;

		boolean emitting;

		boolean missed;

		long requested;

		Producer producer;

		boolean innerActive;

		SwitchSubscriber(ContinuousSubscriber<? super T> child) {
			this.child = child;
			ssub = new SerialSubscription();
			index = new AtomicLong();
			queue = new SpscLinkedArrayQueue<>(RxRingBuffer.SIZE);
			nl = NotificationLite.instance();
		}

		void init() {
			child.add(ssub);
			child.add(Subscriptions.create(this::clearProducer));
			child.setProducer(n -> {
				if (n > 0L) {
					childRequested(n);
				} else if (n < 0L) {
					throw new IllegalArgumentException("n >= 0 expected but it was " + n);
				}
			});
		}

		void clearProducer() {
			synchronized (this) {
				producer = null;
			}
		}

		@Override
		public void onNext(Continuous<? extends T> value) {
			long id = index.incrementAndGet();

			Subscription s = ssub.get();
			if (s != null) {
				s.unsubscribe();
			}

			ContinuousOperatorSwitch.InnerSubscriber<T> inner;

			synchronized (this) {
				inner = new ContinuousOperatorSwitch.InnerSubscriber<T>(id, this);

				innerActive = true;
				producer = null;
			}
			ssub.set(inner);

			value.unsafeSubscribe(inner);
		}

		void emit(T value, ContinuousOperatorSwitch.InnerSubscriber<T> inner) {
			synchronized (this) {
				if (index.get() != inner.id) {
					return;
				}

				queue.offer(inner, nl.next(value));
			}
			drain();
		}

		void innerProducer(Producer p, long id) {
			long n;
			synchronized (this) {
				if (index.get() != id) {
					return;
				}
				n = requested;
				producer = p;
			}

			p.request(n);
		}

		void childRequested(long n) {
			Producer p;
			synchronized (this) {
				p = producer;
				requested = BackpressureUtils.addCap(requested, n);
			}
			if (p != null) {
				p.request(n);
			}
			drain();
		}

		void drain() {
			long localRequested;
			synchronized (this) {
				if (emitting) {
					missed = true;
					return;
				}
				emitting = true;
				localRequested = requested;
			}

			SpscLinkedArrayQueue<Object> localQueue = queue;
			AtomicLong localIndex = index;
			ContinuousSubscriber<? super T> localChild = child;

			for (; ; ) {

				long localEmission = 0L;

				while (localEmission != localRequested) {
					if (localChild.isUnsubscribed()) {
						return;
					}

					boolean empty = localQueue.isEmpty();

					if (empty) {
						break;
					}

					@SuppressWarnings("unchecked")
					ContinuousOperatorSwitch.InnerSubscriber<T> inner = (ContinuousOperatorSwitch.InnerSubscriber<T>) localQueue.poll();
					T value = nl.getValue(localQueue.poll());

					if (localIndex.get() == inner.id) {
						localChild.onNext(value);
						localEmission++;
					}
				}

				if (localEmission == localRequested) {
					if (localChild.isUnsubscribed()) {
						return;
					}

				}

				synchronized (this) {

					localRequested = requested;
					if (localRequested != Long.MAX_VALUE) {
						localRequested -= localEmission;
						requested = localRequested;
					}

					if (!missed) {
						emitting = false;
						return;
					}
					missed = false;
				}
			}
		}

	}

	static final class InnerSubscriber<T> extends ContinuousSubscriber<T> {

		private final long id;

		private final ContinuousOperatorSwitch.SwitchSubscriber<T> parent;

		InnerSubscriber(long id, ContinuousOperatorSwitch.SwitchSubscriber<T> parent) {
			this.id = id;
			this.parent = parent;
		}

		@Override
		public void setProducer(Producer p) {
			parent.innerProducer(p, id);
		}

		@Override
		public void onNext(T t) {
			parent.emit(t, this);
		}

	}

}
