package com.lorentzos.rxexperiment.continuous;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscriber;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class ContinuousRetryOperatorTest {

	@Test
	public void retrySuccess() {
		int NUM_FAILURES = 1;
		int NUM_RETRIES = 3;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				NUM_RETRIES,
				(integer, throwable) -> true);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber).onNext("onSuccessOnly");
		verify(subscriber, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
	}

	@Test
	public void retryCount() {
		int NUM_FAILURES = 20;
		int NUM_RETRIES = 3;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				NUM_RETRIES,
				(integer, throwable) -> true);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber, never()).onNext("onSuccessOnly");
		verify(subscriber, times(1 + NUM_RETRIES)).onNext("beginningEveryTime");
	}

	@Test
	public void retrySuccessInfinite() {
		int NUM_FAILURES = 1;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				Long.MAX_VALUE,
				(integer, throwable) -> true);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber).onNext("onSuccessOnly");
		verify(subscriber, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
	}

	@Test
	public void infiniteRetry() {
		int NUM_FAILURES = 20;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				Long.MAX_VALUE,
				(integer, throwable) -> true);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber).onNext("onSuccessOnly");
		verify(subscriber, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
	}

	@Test
	public void retryTwicePredicate() {
		int NUM_FAILURES = 20;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				Long.MAX_VALUE,
				(integer, throwable) -> integer <= 2);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber, never()).onNext("onSuccessOnly");
		verify(subscriber, times(1 + 2)).onNext("beginningEveryTime");
	}

	@Test
	public void retryPredicateSuccess() {
		int NUM_FAILURES = 3;
		ContinuousSubscriber<String> subscriber = mockSubscriber();

		ContinuousRetryOperator<String> operator = new ContinuousRetryOperator<>(
				s -> Observable.create(new ObservableWithErrors(NUM_FAILURES)),
				Long.MAX_VALUE,
				(integer, throwable) -> integer <= 5);

		Continuous.just("hi")
				.lift(operator)
				.subscribe(subscriber);

		verify(subscriber).onNext("onSuccessOnly");
		verify(subscriber, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
	}

	private static <T> ContinuousSubscriber<T> mockSubscriber() {
		return spy(new SimpleContinuousSubscriber<>());
	}

	private static class SimpleContinuousSubscriber<T> extends ContinuousSubscriber<T> {

		@Override
		public void onNext(T value) {

		}
	}

	private static class ObservableWithErrors implements Observable.OnSubscribe<String> {

		private final int numFailures;
		private final AtomicInteger count = new AtomicInteger(0);

		ObservableWithErrors(int count) {
			this.numFailures = count;
		}

		@Override
		public void call(Subscriber<? super String> o) {
			int i = count.getAndIncrement();
			o.onNext("beginningEveryTime");
			if (i < numFailures) {
				o.onError(new RuntimeException("forced failure: " + (i + 1)));
			} else {
				o.onNext("onSuccessOnly");
				o.onCompleted();
			}
		}
	}
}