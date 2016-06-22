package com.lorentzos.rxexperiment;

/**
 *
 */

public class ContinuousSubject<T> extends Continuous<T> implements ContinuousObserver<T> {

	private final ContinuousSubjectSubscriptionManager<T> state;

	public static <T> ContinuousSubject<T> create() {
		final ContinuousSubjectSubscriptionManager<T> state = new ContinuousSubjectSubscriptionManager<>();
		state.onTerminated = o -> o.emitFirst(state.getLatest(), state.nl);

		return new ContinuousSubject<>(state, state);
	}

	private ContinuousSubject(OnSubscribe<T> onSubscribe, ContinuousSubjectSubscriptionManager<T> state) {
		super(onSubscribe);
		this.state = state;
	}

	@Override
	public void onNext(T value) {
		for (ContinuousSubjectSubscriptionManager.SubjectObserver<T> bo : state.observers()) {
			bo.onNext(value);
		}
	}

	public boolean hasObservers() {
		return state.observers().length > 0;
	}
}
