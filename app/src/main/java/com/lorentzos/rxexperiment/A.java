package com.lorentzos.rxexperiment;

import com.lorentzos.rxexperiment.continuous.ContinuousSubject;
import com.lorentzos.rxexperiment.continuous.ContinuousSubscriber;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func2;

/**
 * onRefreshPayments()
 * .startWith((Void) null)
 * .doOnNext(tick -> view.setRefreshEnabled(true))
 * .switchMap(onRefresh -> openPaymentsModel.getOpenPayments())
 * .retryWhen(new RetryWithConnectivity(view.getContext()))
 * .retryWhen(observable -> observable.flatMap(throwable -> retryObservable))
 * .observeOn(mainScheduler)
 * .doOnUnsubscribe(() -> view.setRefreshEnabled(false))
 * .subscribe(
 * ViewActionSubscriber.create(this::handlePayments, "OnRefresh OpenPayments failed")
 * );
 */

public class A {

	static int i = 0;

	public static void main(String[] args) {

		ContinuousSubject<String> holyMolly = ContinuousSubject.create();

		Subscription subscription = holyMolly
				.flatMapRetry(o -> Observable.create(new Observable.OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> subscriber) {

						if (i++ < 3) {
							System.out.println("'failing'");
							subscriber.onError(new RuntimeException());
						} else {
							subscriber.onNext("hi");
						}
					}
				}), 2)
				.subscribe(new ContinuousSubscriber<String>() {
					@Override
					public void onNext(String s) {
						System.out.println(s);
					}
				});





		Observable.just(1)
				.retry()
				.retry(2)
				.retry(new Func2<Integer, Throwable, Boolean>() {
					@Override
					public Boolean call(Integer integer, Throwable throwable) {
						return null;
					}
				});

		holyMolly.onNext("hello");

		subscription.unsubscribe();

	}

}



