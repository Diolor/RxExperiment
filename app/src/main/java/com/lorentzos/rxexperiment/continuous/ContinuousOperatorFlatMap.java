package com.lorentzos.rxexperiment.continuous;

import com.lorentzos.rxexperiment.continuous.queue.SpscArrayQueue;
import com.lorentzos.rxexperiment.continuous.queue.SpscExactArrayQueue;
import com.lorentzos.rxexperiment.continuous.queue.SpscLinkedArrayQueue;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.functions.Func1;

import static rx.internal.util.unsafe.Pow2.isPowerOfTwo;

/**
 *
 */
public class ContinuousOperatorFlatMap<T, R> implements ContinuousOperator<R, T> {

	private final Func1<? super T, ? extends Continuous<? extends R>> mapper;
	private final int maxConcurrency;
	private final int bufferSize;

	public ContinuousOperatorFlatMap(Func1<? super T, ? extends Continuous<? extends R>> mapper, int maxConcurrency, int bufferSize) {
		this.mapper = mapper;
		this.maxConcurrency = maxConcurrency;
		this.bufferSize = bufferSize;
	}

	@Override
	public ContinuousSubscriber<? super T> call(ContinuousSubscriber<? super R> child) {
		return new MergeSubscriber<>(child, mapper, maxConcurrency, bufferSize);
	}

	static final class MergeSubscriber<T, R> extends ContinuousSubscriber<T> {
		/** */
		private static final long serialVersionUID = -2117620485640801370L;

		final AtomicInteger atomicInteger = new AtomicInteger();
		final ContinuousSubscriber<? super R> actual;
		final Func1<? super T, ? extends Continuous<? extends R>> mapper;
		final int maxConcurrency;
		final int bufferSize;

		volatile Queue<R> queue;

		final AtomicReference<InnerSubscriber<?, ?>[]> subscribers;

		static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];

		long lastId;
		int lastIndex;

		Queue<Continuous<? extends R>> sources;

		int wip;

		public MergeSubscriber(ContinuousSubscriber<? super R> actual, Func1<? super T, ? extends Continuous<? extends R>> mapper, int maxConcurrency, int bufferSize) {
			this.actual = actual;
			this.mapper = mapper;
			this.maxConcurrency = maxConcurrency;
			this.bufferSize = bufferSize;
			if (maxConcurrency != Integer.MAX_VALUE) {
				sources = new ArrayDeque<>(maxConcurrency);
			}
			subscribers = new AtomicReference<>(EMPTY);
		}

		@Override
		public void onNext(T t) {
			// safeguard against misbehaving sources

			Continuous<? extends R> p = mapper.call(t);

			//			if (p instanceof NbpObservableScalarSource) {
			//				tryEmitScalar(((NbpObservableScalarSource<? extends R>) p).value());
			//			} else {
			if (maxConcurrency == Integer.MAX_VALUE) {
				subscribeInner(p);
			} else {
				synchronized (this) {
					if (wip == maxConcurrency) {
						sources.offer(p);
						return;
					}
					wip++;
				}
				subscribeInner(p);
				//				}
			}
		}

		void subscribeInner(Continuous<? extends R> p) {
			InnerSubscriber<T, R> inner = new InnerSubscriber<>(this);
			addInner(inner);
			p.subscribe(inner);
		}

		void addInner(InnerSubscriber<T, R> inner) {
			for (; ; ) {
				InnerSubscriber<?, ?>[] a = subscribers.get();
				int n = a.length;
				InnerSubscriber<?, ?>[] b = new InnerSubscriber[n + 1];
				System.arraycopy(a, 0, b, 0, n);
				b[n] = inner;
				if (subscribers.compareAndSet(a, b)) {
					return;
				}
			}
		}

		void removeInner(InnerSubscriber<T, R> inner) {
			for (; ; ) {
				InnerSubscriber<?, ?>[] a = subscribers.get();
				int n = a.length;
				int j = -1;
				for (int i = 0; i < n; i++) {
					if (a[i] == inner) {
						j = i;
						break;
					}
				}
				if (j < 0) {
					return;
				}
				InnerSubscriber<?, ?>[] b;
				if (n == 1) {
					b = EMPTY;
				} else {
					b = new InnerSubscriber<?, ?>[n - 1];
					System.arraycopy(a, 0, b, 0, j);
					System.arraycopy(a, j + 1, b, j, n - j - 1);
				}
				if (subscribers.compareAndSet(a, b)) {
					return;
				}
			}
		}

		Queue<R> getMainQueue() {
			Queue<R> q = queue;
			if (q == null) {
				if (maxConcurrency == Integer.MAX_VALUE) {
					q = new SpscLinkedArrayQueue<R>(bufferSize);
				} else {
					if (isPowerOfTwo(maxConcurrency)) {
						q = new SpscArrayQueue<R>(maxConcurrency);
					} else {
						q = new SpscExactArrayQueue<R>(maxConcurrency);
					}
				}
				queue = q;
			}
			return q;
		}

		void tryEmit(R value, InnerSubscriber<T, R> inner) {
			if (atomicInteger.get() == 0 && atomicInteger.compareAndSet(0, 1)) {
				actual.onNext(value);
				if (atomicInteger.decrementAndGet() == 0) {
					return;
				}
			} else {
				Queue<R> q = inner.queue;
				if (q == null) {
					q = new SpscLinkedArrayQueue<>(bufferSize);
					inner.queue = q;
				}
				if (atomicInteger.getAndIncrement() != 0) {
					return;
				}
			}
			drainLoop();
		}

		void tryEmitScalar(R value) {
			if (atomicInteger.get() == 0 && atomicInteger.compareAndSet(0, 1)) {
				actual.onNext(value);
				if (atomicInteger.decrementAndGet() == 0) {
					return;
				}
			} else {
				Queue<R> q = getMainQueue();
				if (!q.offer(value)) {
					//					onError(new IllegalStateException("Scalar queue full?!"));
					return;
				}
				if (atomicInteger.getAndIncrement() != 0) {
					return;
				}
			}
			drainLoop();
		}

		void drainLoop() {
			ContinuousSubscriber<? super R> child = actual;
			int missed = 1;
			for (; ; ) {
				Queue<R> svq = queue;

				if (svq != null) {
					for (; ; ) {
						R o;
						for (; ; ) {
							o = svq.poll();
							if (o == null) {
								break;
							}

							child.onNext(o);
						}
						if (o == null) {
							break;
						}
					}
				}

				svq = queue;
				InnerSubscriber<?, ?>[] inner = subscribers.get();
				int n = inner.length;

				if ((svq == null || svq.isEmpty()) && n == 0) {
					return;
				}

				boolean innerCompleted = false;
				if (n != 0) {
					int index = lastIndex;

					if (n <= index) {
						if (n <= index) {
							index = 0;
						}
						int j = index;
						for (int i = 0; i < n; i++) {
							j++;
							if (j == n) {
								j = 0;
							}
						}
						index = j;
						lastIndex = j;
					}

					int j = index;
					for (int i = 0; i < n; i++) {
						@SuppressWarnings("unchecked")
						InnerSubscriber<T, R> is = (InnerSubscriber<T, R>) inner[j];

						R o = null;
						for (; ; ) {
							for (; ; ) {
								Queue<R> q = is.queue;
								if (q == null) {
									break;
								}
								o = q.poll();
								if (o == null) {
									break;
								}

								child.onNext(o);
							}
							if (o == null) {
								break;
							}
						}
						Queue<R> innerQueue = is.queue;
						if (innerQueue == null || innerQueue.isEmpty()) {
							removeInner(is);
							innerCompleted = true;
						}

						j++;
						if (j == n) {
							j = 0;
						}
					}
					lastIndex = j;
				}

				if (innerCompleted) {
					if (maxConcurrency != Integer.MAX_VALUE) {
						Continuous<? extends R> p;
						synchronized (this) {
							p = sources.poll();
							if (p == null) {
								wip--;
								continue;
							}
						}
						subscribeInner(p);
					}
					continue;
				}
				missed = atomicInteger.addAndGet(-missed);
				if (missed == 0) {
					break;
				}
			}
		}

		@Override
		public void unsubscribe() {
			InnerSubscriber<?, ?>[] a = subscribers.get();
			for (InnerSubscriber<?, ?> inner : a) {
				inner.unsubscribe();
			}
		}

	}

	static final class InnerSubscriber<T, U> extends ContinuousSubscriber<U> {
		final MergeSubscriber<T, U> parent;

		volatile Queue<U> queue;

		public InnerSubscriber(MergeSubscriber<T, U> parent) {
			this.parent = parent;
		}

		@Override
		public void onNext(U t) {
			parent.tryEmit(t, this);
		}

	}

}
