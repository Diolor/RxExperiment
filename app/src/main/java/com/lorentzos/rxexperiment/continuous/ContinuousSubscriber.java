package com.lorentzos.rxexperiment.continuous;

import rx.Producer;
import rx.Subscription;
import rx.internal.util.SubscriptionList;

/**
 *
 */
public abstract class ContinuousSubscriber<T> implements ContinuousObserver<T>, Subscription {

	// represents requested not set yet
	private static final Long NOT_SET = Long.MIN_VALUE;
	private final SubscriptionList subscriptions;
	private final ContinuousSubscriber<?> subscriber;
	/* protected by `this` */
	private Producer producer;
	/* protected by `this` */
	private long requested = NOT_SET; // default to not set

	protected ContinuousSubscriber() {
		this(null, false);
	}

	protected ContinuousSubscriber(ContinuousSubscriber<?> subscriber) {
		this(subscriber, true);
	}

	protected ContinuousSubscriber(ContinuousSubscriber<?> subscriber, boolean shareSubscriptions) {
		this.subscriber = subscriber;
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

	public void setProducer(Producer p) {
		long toRequest;
		boolean passToSubscriber = false;
		synchronized (this) {
			toRequest = requested;
			producer = p;
			if (subscriber != null) {
				// middle operator ... we pass through unless a request has been made
				if (toRequest == NOT_SET) {
					// we pass through to the next producer as nothing has been requested
					passToSubscriber = true;
				}
			}
		}
		// do after releasing lock
		if (passToSubscriber) {
			subscriber.setProducer(producer);
		} else {
			// we execute the request with whatever has been requested (or Long.MAX_VALUE)
			if (toRequest == NOT_SET) {
				producer.request(Long.MAX_VALUE);
			} else {
				producer.request(toRequest);
			}
		}
	}
}
