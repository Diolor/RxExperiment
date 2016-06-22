package com.lorentzos.rxexperiment;

/**
 *
 */

public class B {

//	public static void main(String[] args) {
//
//		int i = 0;
//
//		System.out.println(++i);
//
//		Observable.create(new Observable.NbpOnSubscribe<String>() {
//			@Override
//			public void accept(Observer<? super String> observer) {
//
//			}
//		});

//		Observable.create(new Observable.OnSubscribe<String>() {
//
//			int i;
//
//			@Override
//			public void call(Subscriber<? super String> subscriber) {
//				if (i < 8) {
//					i++;
//					subscriber.onError(new TheBException());
//				} else {
//					subscriber.onNext("Success");
//					subscriber.onCompleted();
//				}
//			}
//		})
//				.retryWhen(new ExponentialBackoff())
//				.subscribe(new Subscriber<Object>() {
//					@Override
//					public void onCompleted() {
//						System.out.println("onCompleted");
//					}
//
//					@Override
//					public void onError(Throwable e) {
//						System.out.println("onError " + e.getMessage());
//					}
//
//					@Override
//					public void onNext(Object o) {
//						System.out.println("onNext " + o);
//					}
//				});
//
//	}

//	public static class ExponentialBackoff implements Func1<Observable<? extends Throwable>, Observable<?>> {
//
//		private final int maxDelay;
//		private int retries;
//
//		public ExponentialBackoff() {
//			maxDelay = 60;
//		}
//
//		public ExponentialBackoff(int maxDelay) {
//			this.maxDelay = maxDelay;
//		}
//
//		@Override
//		public Observable<?> call(Observable<? extends Throwable> inputObservable) {
//
//			// it is critical to use inputObservable in the chain for the result
//			// ignoring it and doing your own thing will break the sequence
//
//			return inputObservable.flatMap((Func1<Throwable, Observable<?>>) throwable -> {
//
//				int delay = (int) StrictMath.pow(2, retries++);
////				retries++;
//
//				if (delay > maxDelay) {
//					delay = maxDelay;
//				}
//
//				System.out.println(String.format(Locale.US, "Retrying in %d sec", delay));
//
//				return Observable.timer(delay, TimeUnit.SECONDS, immediate());
//				//						.doOnCompleted(() -> retries++);
//			});
//		}
//
//	}

	static class TheBException extends Exception {
	}
}
