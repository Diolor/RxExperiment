package com.lorentzos.rxexperiment.continuous;

import java.util.Objects;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 *
 */
public class Continuous<T> {

	final OnSubscribe<T> onSubscribe;

	Continuous(OnSubscribe<T> onSubscribe) {
		this.onSubscribe = onSubscribe;
	}

	interface OnSubscribe<T> extends Action1<ContinuousSubscriber<? super T>> {
		// cover for generics insanity
	}

	//		@SuppressWarnings({"unchecked", "rawtypes"})
	//		public static <T> Continuous<T> concatArray(Continuous<? extends T>... sources) {
	//			if (sources.length == 0) {
	//				//				return empty(); // TODO: 21/06/16
	//			} else if (sources.length == 1) {
	//				return (Continuous<T>) sources[0];
	//			}
	//			return fromArray(sources).concatMap((Func1) Functions.identity());
	//		}

	//		public final <R> Continuous<R> concatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper) {
	//			return concatMap(mapper, 2);
	//		}

	//		public final <R> Continuous<R> concatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper, int prefetch) {
	//			if (prefetch <= 0) {
	//				throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
	//			}
	//
	//			return lift(new ContinuousOperatorConcatMap<>(mapper, prefetch));
	//		}

	public static <T> Continuous<T> create(OnSubscribe<T> onSubscribe) {
		return new Continuous<>(onSubscribe);
	}

	public final Continuous<T> doOnNext(Action1<? super T> onNext) {
		Objects.requireNonNull(onNext, "onNext is null");
		return lift(new ContinuousOperatorDoOnNext<>(onNext));
	}

	public <R> Continuous<R> flatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper) {
		return flatMap(mapper, Integer.MAX_VALUE);
	}

	public final <R> Continuous<R> flatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper, int maxConcurrency) {
		return flatMap(mapper, maxConcurrency, Integer.MAX_VALUE);
	}

	public final <R> Continuous<R> flatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper,
	                                       int maxConcurrency, int bufferSize) {
		Objects.requireNonNull(mapper, "mapper should not be null");
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
		}
		validateBufferSize(bufferSize);
		//		if (this instanceof ObservableScalarSource) {
		//			ObservableScalarSource<T> scalar = (ObservableScalarSource<T>) this;
		//			return create(scalar.scalarFlatMap(mapper));
		//		}
		return lift(new ContinuousOperatorFlatMap<>(mapper, maxConcurrency, bufferSize));
	}

	public Continuous<T> flatMapRetry(Func1<? super T, ? extends Observable<? extends T>> mapper) {
		return flatMapRetry(mapper, Long.MAX_VALUE, (count, e) -> true);
	}

	public Continuous<T> flatMapRetry(Func1<? super T, ? extends Observable<? extends T>> mapper, Func2<Integer, Throwable, Boolean> predicate) {
		return lift(new ContinuousRetryOperator<>(mapper, Long.MAX_VALUE, predicate));
	}

	public Continuous<T> flatMapRetry(Func1<? super T, ? extends Observable<? extends T>> mapper, int times) {
		return lift(new ContinuousRetryOperator<>(mapper, times, (count, e) -> true));
	}

	public Continuous<T> flatMapRetry(Func1<? super T, ? extends Observable<? extends T>> mapper, long times, Func2<Integer, Throwable, Boolean> predicate) {
		return lift(new ContinuousRetryOperator<>(mapper, times, predicate));
	}

	public static <T> Continuous<T> fromArray(T... values) {
		Objects.requireNonNull(values, "values is null");
		if (values.length == 0) {
			//				return empty(); // TODO: 21/06/16
		} else if (values.length == 1) {
			return just(values[0]);
		}
		return create(new ContinuousOnSubscribeArraySource<>(values));
	}

	public static <T> Continuous<T> just(T value) {
		Objects.requireNonNull(value, "The value is null");
		return new ContinuousObservableScalarSource<T>(value);
	}

	@SuppressWarnings("unchecked")
	public static <T> Continuous<T> just(T v1, T v2) {
		Objects.requireNonNull(v1, "The first value is null");
		Objects.requireNonNull(v2, "The second value is null");

		return fromArray(v1, v2);
	}

	public final <R> Continuous<R> map(Func1<? super T, ? extends R> mapper) {
		Objects.requireNonNull(mapper, "mapper is null");
		return lift(new ContinuousOperatorMap<R, T>(mapper));
	}

	//	@SuppressWarnings("unchecked")
	//	public static <T> Observable<T> merge(Observable<? extends T> t1, Observable<? extends T> t2) {
	//		return merge(new Observable[] { t1, t2 });
	//	}

	//	public static <T> Observable<T> merge(Continuous<? extends Continuous<? extends T>> source) {
	//		Objects.requireNonNull(source, "source is null");
	//
	//		return source.lift(OperatorMerge.instance(false));
	//	}

	public final <R> Continuous<R> lift(ContinuousOperator<? extends R, ? super T> onLift) {
		Objects.requireNonNull(onLift, "onLift is null");
		return create(new ContinuousOnSubscribeLift<>(this, onLift));
	}

	//		@SuppressWarnings("unchecked")
	//		public final Continuous<T> startWith(T value) {
	//			Objects.requireNonNull(value, "value is null");
	//			return concatArray(just(value), this);
	//		}

	public final Subscription subscribe(ContinuousSubscriber<? super T> subscriber) {
		// validate and proceed
		Objects.requireNonNull(subscriber, "continuousSubscriber can not be null");
		Objects.requireNonNull(onSubscribe, "onSubscribe function can not be null.");

		onSubscribe.call(subscriber);
		return subscriber;
	}

	public final Subscription subscribe(Subscriber<? super T> subscriber) {
		// validate and proceed
		Objects.requireNonNull(subscriber, "continuousSubscriber can not be null");
		Objects.requireNonNull(onSubscribe, "onSubscribe function can not be null.");

		subscriber.onStart();

		ContinuousSubscriber<T> continuousSubscriber = new ContinuousSubscriber<T>() {
			@Override
			public void onNext(T value) {
				subscriber.onNext(value);
			}

			@Override
			public void unsubscribe() {
				super.unsubscribe();
			}
		};
		subscriber.add(continuousSubscriber);
		continuousSubscriber.add(subscriber);

		onSubscribe.call(continuousSubscriber);
		return subscriber;
	}

	/** Same code as {@link #subscribe(ContinuousSubscriber)} */
	public final Subscription unsafeSubscribe(ContinuousSubscriber<? super T> subscriber) {

		return subscribe(subscriber);
	}

	public static <T> Continuous<T> switchOnNext(Continuous<? extends Continuous<? extends T>> sequenceOfSequences) {
		return sequenceOfSequences.lift(ContinuousOperatorSwitch.<T>instance());
	}

	private static void validateBufferSize(int bufferSize) {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
		}
	}
}
