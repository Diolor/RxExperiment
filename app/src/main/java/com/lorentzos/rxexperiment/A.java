package com.lorentzos.rxexperiment;

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
	public static void main(String[] args) {

		ContinuousSubject<String> holyMolly = ContinuousSubject.create();

		holyMolly
//				.doOnNext(System.out::println)
				.map(s -> 1)
				.subscribe(new ContinuousSubscriber<Integer>() {
					@Override
					public void onNext(Integer s) {
						System.out.println(s);
					}
				});

		holyMolly.subscribe(new ContinuousSubscriber<String>() {
			@Override
			public void onNext(String value) {
				System.out.println(value);
			}
		});

		holyMolly.onNext("hello");
	}

}



