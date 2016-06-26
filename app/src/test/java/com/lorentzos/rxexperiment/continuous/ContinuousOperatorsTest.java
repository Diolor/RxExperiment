package com.lorentzos.rxexperiment.continuous;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subscriptions.Subscriptions;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ContinuousOperatorsTest {

	@Test
	public void doOnNext() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();
		final AtomicBoolean onNext = new AtomicBoolean();

		Continuous.just("A")
				.doOnNext(s -> onNext.set(true))
				.subscribe(ts);
		assertTrue(onNext.get());
	}

	@Test
	public void flatMap() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();

		final AtomicBoolean unsubscribed = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);

		Subscription subscription = Continuous.just("A")
				.flatMap(value ->
						Continuous.create(new Continuous.OnSubscribe<String>() {
							@Override
							public void call(ContinuousSubscriber<? super String> s) {

								s.onNext(value + "B");

								s.add(Subscriptions.create(() -> {
									unsubscribed.set(true);
									latch.countDown();
								}));
							}
						}))
				.subscribe(ts);

		subscription.unsubscribe();

		latch.await(2, TimeUnit.SECONDS);

		assertTrue(subscription.isUnsubscribed());
		assertTrue(unsubscribed.get());
	}

	@Test
	public void map() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();
		Continuous.just("A")
				.map(s -> s + "B")
				.subscribe(ts);
		ts.assertValue("AB");
	}

}