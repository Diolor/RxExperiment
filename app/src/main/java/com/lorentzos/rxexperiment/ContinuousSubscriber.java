package com.lorentzos.rxexperiment;

import rx.Subscription;
import rx.internal.util.SubscriptionList;

/**
 *
 */
public abstract class ContinuousSubscriber<T> implements ContinuousObserver<T>, Subscription {

	private final SubscriptionList subscriptions;

	protected ContinuousSubscriber() {
		this(null, false);
	}

	protected ContinuousSubscriber(ContinuousSubscriber<?> subscriber) {
		this(subscriber, true);
	}

	protected ContinuousSubscriber(ContinuousSubscriber<?> subscriber, boolean shareSubscriptions) {
		subscriptions = shareSubscriptions && subscriber != null ? subscriber.subscriptions : new SubscriptionList();
	}

	public final void add(Subscription s) {
		subscriptions.add(s);
	}

	@Override
	public void unsubscribe() {
		subscriptions.unsubscribe();
	}

	@Override
	public boolean isUnsubscribed() {
		return subscriptions.isUnsubscribed();
	}
}
