package com.lorentzos.rxexperiment;

import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

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

	public final <R> Continuous<R> lift(ContinuousOperator<? extends R, ? super T> onLift) {
		Objects.requireNonNull(onLift, "onLift is null");
		return create(new ContinuousOnSubscribeLift<>(this, onLift));
	}

	//		@SuppressWarnings("unchecked")
	//		public final Continuous<T> startWith(T value) {
	//			Objects.requireNonNull(value, "value is null");
	//			return concatArray(just(value), this);
	//		}

	public final Subscription subscribe(ContinuousSubscriber<? super T> continuousSubscriber) {
		// validate and proceed
		Objects.requireNonNull(continuousSubscriber, "continuousSubscriber can not be null");
		Objects.requireNonNull(onSubscribe, "onSubscribe function can not be null.");

		onSubscribe.call(continuousSubscriber);
		return continuousSubscriber;
	}

}
