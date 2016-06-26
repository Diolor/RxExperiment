package com.lorentzos.rxexperiment.continuous;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 *
 */
public class ContinuousRetryOperator<T> implements ContinuousOperator<T, T> {

	private final Func1<? super T, ? extends Observable<? extends T>> mapper;
	private final Func2<Integer, Throwable, Boolean> predicate;
	private final long remaining;

	public ContinuousRetryOperator(Func1<? super T, ? extends Observable<? extends T>> mapper, long remaining, Func2<Integer, Throwable, Boolean> predicate) {
		this.mapper = mapper;
		this.remaining = remaining;
		this.predicate = predicate;
	}

	@Override
	public ContinuousSubscriber<? super T> call(ContinuousSubscriber<? super T> child) {
		return new RedoContinuousSubscriber<>(child, mapper, remaining, predicate);
	}

	/**
	 *
	 */
	public static class RedoContinuousSubscriber<T> extends ContinuousSubscriber<T> {
		private final ContinuousSubscriber<? super T> child;
		private final Func1<? super T, ? extends Observable<? extends T>> mapper;
		private long remaining;
		private final AtomicInteger atomicInteger = new AtomicInteger();
		private final Func2<Integer, Throwable, Boolean> predicate;
		int retries;

		public RedoContinuousSubscriber(ContinuousSubscriber<? super T> child, Func1<? super T, ? extends Observable<? extends T>> mapper, long remaining, Func2<Integer, Throwable, Boolean> predicate) {

			this.child = child;
			this.mapper = mapper;
			this.remaining = remaining;
			this.predicate = predicate;
		}

		public void onNext(T value) {
			if (isUnsubscribed()) {
				return;
			}

			Observable<? extends T> observable = mapper.call(value);
			observable.subscribe(new Subscriber<T>() {
				@Override
				public void onCompleted() {
					// we don't care
				}

				@Override
				public void onError(Throwable e) {
					long r = remaining;
					if (r != Long.MAX_VALUE) {
						remaining = r - 1;
					}
					if (r == 0) {
						return;
					}

					boolean condition = predicate.call(++retries, e);
					if (condition) {
						subscribeNext(observable, this);
					}
				}

				@Override
				public void onNext(T t) {
					child.onNext(t);
				}
			});

		}

		void subscribeNext(Observable<? extends T> observable, Subscriber<T> subscriber) {
			if (atomicInteger.getAndIncrement() == 0) {
				int missed = 1;
				for (; ; ) {
					observable.subscribe(subscriber);

					missed = atomicInteger.addAndGet(-missed);
					if (missed == 0) {
						break;
					}
				}
			}
		}
	}
}