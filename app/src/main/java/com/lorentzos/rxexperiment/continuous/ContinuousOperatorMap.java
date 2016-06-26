package com.lorentzos.rxexperiment.continuous;

import rx.functions.Func1;

/**
 *
 */
public class ContinuousOperatorMap<Downstream, Upstream> implements ContinuousOperator<Downstream, Upstream> {

	private final Func1<? super Upstream, ? extends Downstream> mapper;

	public ContinuousOperatorMap(Func1<? super Upstream, ? extends Downstream> mapper) {
		this.mapper = mapper;
	}

	@Override
	public ContinuousSubscriber<? super Upstream> call(ContinuousSubscriber<? super Downstream> child) {
		ContinuousSubscriber<Upstream> parent = new ContinuousSubscriber<Upstream>(child) {
			@Override
			public void onNext(Upstream upstream) {
				Downstream downstream = mapper.call(upstream);
				child.onNext(downstream);
			}
		};
		child.add(parent);
		return parent;
	}
}
