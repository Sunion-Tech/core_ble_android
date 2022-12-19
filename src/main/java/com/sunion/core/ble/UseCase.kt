package com.sunion.core.ble

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

interface UseCase {

    interface ReactiveFlowable<I, O> {
        operator fun invoke(input: I): Flowable<O>
    }

    interface ReactiveObservable<I, O> {
        operator fun invoke(input: I): Observable<O>
    }

    interface ReactiveSingle<I, O> {
        operator fun invoke(input: I): Single<O>
    }

    interface ReactiveMaybe<I, O> {
        operator fun invoke(input: I): Maybe<O>
    }

    interface Execute<I, O> {
        operator fun invoke(input: I): O
    }

    interface ExecuteArgument2<I1, I2, O> {
        operator fun invoke(input1: I1, input2: I2): O
    }

    interface ExecuteArgument3<I1, I2, I3, O> {
        operator fun invoke(input1: I1, input2: I2, input3: I3): O
    }

    interface ExecuteArgument4<I1, I2, I3, I4, O> {
        operator fun invoke(input1: I1, input2: I2, input3: I3, input4: I4): O
    }

    interface ExecuteArgument5<I1, I2, I3, I4, I5, O> {
        operator fun invoke(input1: I1, input2: I2, input3: I3, input4: I4, input5: I5): O
    }

    interface JustExecute<I> {
        operator fun invoke(input: I)
    }

    interface JustExecuteArgument2<I, I2> {
        operator fun invoke(input1: I, input2: I2)
    }

    interface JustExecuteArgument3<I, I2, I3> {
        operator fun invoke(input1: I, input2: I2, input3: I3)
    }
}