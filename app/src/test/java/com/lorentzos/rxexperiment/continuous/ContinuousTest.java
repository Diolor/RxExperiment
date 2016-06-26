package com.lorentzos.rxexperiment.continuous;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subscriptions.Subscriptions;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class ContinuousTest {

	@Test
	public void helloWold() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();
		Continuous.just("Hello World!").subscribe(ts);
		ts.assertValue("Hello World!");

	}

	@Test
	public void testCreateSuccess() {
		TestSubscriber<Object> ts = new TestSubscriber<>();

		Continuous.create(s -> s.onNext("Hello")).subscribe(ts);

		ts.assertValue("Hello");
	}

	@Test(expected = RuntimeException.class)
	public void testCreateError() {
		TestSubscriber<Object> ts = new TestSubscriber<>();
		Continuous.create(s -> {
			throw new RuntimeException("Error");
		}).subscribe(ts);
	}

	@Test
	public void testUnsubscribe() throws InterruptedException {
		TestSubscriber<String> ts = new TestSubscriber<>();
		final AtomicBoolean unsubscribed = new AtomicBoolean();
		final AtomicBoolean interrupted = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(2);

		Continuous<String> s1 = Continuous.create(s -> {

			final Thread t = new Thread(() -> {
				try {
					Thread.sleep(5000);
					s.onNext("should not be displayed");
				} catch (InterruptedException e) {
					interrupted.set(true);
					latch.countDown();
				}
			});

			s.add(Subscriptions.create(() -> {
				unsubscribed.set(true);
				t.interrupt();
				latch.countDown();
			}));
			t.start();
		});

		Subscription subscription = s1.subscribe(ts);

		Thread.sleep(100);

		subscription.unsubscribe();

		if (latch.await(1000, TimeUnit.MILLISECONDS)) {
			ts.assertNoValues();
			assertTrue(unsubscribed.get());
			assertTrue(subscription.isUnsubscribed());
			assertTrue(interrupted.get());
		} else {
			fail("timed out waiting for latch");
		}
	}

	@Test
	public void testUnsubscribe2() throws InterruptedException {
		ContinuousSubscriber<String> continuousSubscriber = new ContinuousSubscriber<String>() {
			@Override
			public void onNext(String value) {

			}
		};
		final AtomicBoolean unsubscribed = new AtomicBoolean();
		final AtomicBoolean interrupted = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(2);

		Continuous<String> s1 = Continuous.create(s -> {

			final Thread t = new Thread(() -> {
				try {
					Thread.sleep(5000);
					s.onNext("should not be displayed");
				} catch (InterruptedException e) {
					interrupted.set(true);
					latch.countDown();
				}
			});

			s.add(Subscriptions.create(() -> {
				unsubscribed.set(true);
				t.interrupt();
				latch.countDown();
			}));
			t.start();
		});

		Subscription subscription = s1.subscribe(continuousSubscriber);

		Thread.sleep(100);

		subscription.unsubscribe();

		if (latch.await(1000, TimeUnit.MILLISECONDS)) {
			assertTrue(unsubscribed.get());
			assertTrue(subscription.isUnsubscribed());
			assertTrue(interrupted.get());
		} else {
			fail("timed out waiting for latch");
		}
	}

	@Test
	public void testUnsubscribe3() throws InterruptedException {
		Subscriber<String> subscriber = new Subscriber<String>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(String value) {

			}
		};
		final AtomicBoolean unsubscribed = new AtomicBoolean();
		final AtomicBoolean interrupted = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(2);

		Continuous<String> s1 = Continuous.create(s -> {

			final Thread t = new Thread(() -> {
				try {
					Thread.sleep(5000);
					s.onNext("should not be displayed");
				} catch (InterruptedException e) {
					interrupted.set(true);
					latch.countDown();
				}
			});

			s.add(Subscriptions.create(() -> {
				unsubscribed.set(true);
				t.interrupt();
				latch.countDown();
			}));
			t.start();
		});

		Subscription subscription = s1.subscribe(subscriber);

		Thread.sleep(100);

		subscription.unsubscribe();

		if (latch.await(1000, TimeUnit.MILLISECONDS)) {
			assertTrue(unsubscribed.get());
			assertTrue(subscription.isUnsubscribed());
			assertTrue(interrupted.get());
		} else {
			fail("timed out waiting for latch");
		}
	}

	@Test
	public void fromArray() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();
		String[] items = {"one", "two", "three"};
		Continuous<String> continuous = Continuous.fromArray(items);
		continuous.subscribe(ts);

		ts.assertValues(items);
	}

	@Test
	public void just() throws Exception {
		TestSubscriber<String> ts = new TestSubscriber<>();
		Continuous<String> continuous = Continuous.just("one", "two");
		continuous.subscribe(ts);

		ts.assertValues("one", "two");
	}

//	@Test
//	public void merge() throws Exception {
//		TestSubscriber<String> ts = new TestSubscriber<>();
//		Continuous<String> c1 = Continuous.just("one");
//		Continuous<String> c2 = Continuous.just("two");
//
////		Continuous.merge(c1, c2);
//		c1.subscribe(ts);
//
//		ts.assertValues("one", "two");
//
//	}
}