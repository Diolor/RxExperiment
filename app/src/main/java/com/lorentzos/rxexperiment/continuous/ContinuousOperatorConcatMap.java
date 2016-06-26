package com.lorentzos.rxexperiment.continuous;

import rx.functions.Func1;

/**
 *
 */
public class ContinuousOperatorConcatMap<Downstream, Upstream> implements ContinuousOperator<Downstream, Upstream> {

	private final Func1<? super Upstream, ? extends Continuous<? extends Downstream>> mapper;
	private final int bufferSize;

	public static void main(String[] args) {

		Continuous.just(Continuous.just(1, 2))
				.lift(new ContinuousOperatorConcatMap<>(new Func1<Continuous<Integer>, Continuous<Integer>>() {
					@Override
					public Continuous<Integer> call(Continuous<Integer> integerContinuous) {
						return integerContinuous;
					}
				}, 2)).subscribe(new ContinuousSubscriber<Integer>() {
			@Override
			public void onNext(Integer s) {
				System.out.println(s);
			}
		});

	}

	public ContinuousOperatorConcatMap(Func1<? super Upstream, ? extends Continuous<? extends Downstream>> mapper, int bufferSize) {

		this.mapper = mapper;
		this.bufferSize = Math.max(8, bufferSize);

	}

	@Override
	public ContinuousSubscriber<? super Upstream> call(ContinuousSubscriber<? super Downstream> continuousSubscriber) {


		return null;
	}
}
