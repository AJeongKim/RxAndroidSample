package com.android.rxandroid;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by user on 2017-09-01.
 */

public class SamleObservable {
    public static Observable<String> request() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                ArrayList<String> results = new ArrayList<String>();
                results.add("");
                results.add("rxJava");
                results.add("rxAndroid");
                results.add("rxKotlin");
                for (String s : results) {
                    subscriber.onNext(s);
                }
                subscriber.onCompleted();
            }
        });
    }
}
