package com.lorentzos.rxexperiment;

import rx.functions.Func1;

/**
 *
 */
public interface ContinuousOperator<Downstream, Upstream> extends Func1<ContinuousSubscriber<? super Downstream>, ContinuousSubscriber<? super Upstream>> {
}
