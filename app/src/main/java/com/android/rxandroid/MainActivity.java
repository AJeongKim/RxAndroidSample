package com.android.rxandroid;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

// 옵저러블의 액션 흐름
// onNext > onNext > ... > onNext > onCompleted
// onNext > onNext > ... > onError

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private Context mContext;
    private LinearLayout mResultListLayout;
    private Button mStartBtn;
    private Observable<String> mSimpleObservable;
    private Observable<String> mSimpleObservable2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        startSimpleObservable();
        mResultListLayout = (LinearLayout) findViewById(R.id.rx_api_result_list_layout);
        mStartBtn = (Button) findViewById(R.id.rx_start_btn);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestString();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSimpleObservable != null) {
            mSimpleObservable.subscribe().unsubscribe();
        }
        if (mSimpleObservable2 != null) {
            mSimpleObservable2.subscribe().unsubscribe();
        }
    }

    private void startSimpleObservable() {
        mSimpleObservable =
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        subscriber.onNext("Hello RxAndroid!");
                        subscriber.onCompleted();

                        Log.d(TAG, "Subscribe Current Thread : " + Thread.currentThread().getName());
                    }
                });

        mSimpleObservable2 = Observable.just("Hello RxAndroid222!");

        startSubscribe(mSimpleObservable);
        startSubscribe(mSimpleObservable2);
    }

    /**
     * subscribeOn(), observeOn() 쓰레드 관련 메소드 테스트
     * @param observable
     */
    private void startSubscribe(Observable<String> observable) {
        observable
                // observeOn이 여러번 선언 됐을 경우 각각의 스레드에서 실행
                // 아래 'subscribeOn(Schedulers.computation())'라고 지정한건 아무의미없음
                // 테스트하기 위해 설정
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        // 스트림 완료되어 종료한다.
                        Log.d(TAG, "completed!");
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 에러 신호를 전달한다.
                        Log.e(TAG, "error : " + e.getMessage());
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                        Log.d(TAG, "start!");
                    }

                    @Override
                    public void onNext(String s) {
                        // 새로운 데이터를 전달한다.
                        Log.d(TAG, "next : " + s);
                        Log.d(TAG, "Observe Current Thread : " + Thread.currentThread().getName());
                    }
                });
    }

    /**
     * filter(), map() 메소드 테스트
     */
    private void requestString() {
        SamleObservable.request()
                .filter(new Func1<String, Boolean>() {

                    @Override
                    public Boolean call(String s) {
                        return !s.isEmpty();
                    }
                })
                // map은 한 데이터를 다른 데이터로 바꾸는 오퍼레이터
                // 원본의 데이터는 변경하지 않고 새로운 스트림을 만들어 낸다.
                // 2개 사용할 시 뒤에 사용한 map이 최종 적용
                .map((String text) -> { return text.toLowerCase();})
                .subscribe(new Subscriber<String>() {

                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "request string onCompleted()");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "request string onError() errormessage : " + e.getMessage());
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "request string onNext()");
                        TextView textView = new TextView(mContext);
                        textView.setTextSize(25);
                        textView.setGravity(Gravity.CENTER);
                        textView.setText(s);
                        mResultListLayout.addView(textView);
                    }
                });
    }
}


// 스케쥴러 지정 subscribeOn(), observeOn()
// - subscribeOn()
// 1. observable의 작업을 시작하는 쓰레드 선택가능
// 2. 중복해서 적을 경우 가장 마지막에 적힌 스레드에서 시작
// - observeOn()
// 1. 이후에 나오는 오퍼레이터, subscribe의 스케줄러를 변경할 수 있다.
// - 제공하는 스케쥴러
// 1. Schedulers.computation() : 간단한 연산이나 콜백 처리를 위해 사용,
// RxComputationThreadPool라는 별도의 풀에서 돌아감. 최대 cpu 개수의 쓰레드 풀이 순환하면서 실행
// 2. Schedulers.immediate() : 현재 스레드에서 즉시 수행
// observeOn()이 여러번 쓰였을 경우 immediate()를 선언한 바로 윗쪽의 쓰레드를 따라감
// 3. Schedulers.from(executor) : 특정 executor를 스케쥴러로 사용
// 4. Schedulers.io() : 동기 I/O를 별도로 처리시켜 비동기 효율을 얻기 위한 스케줄러
// 자체적인 스레드풀 CachedThreadPool을 사용. API 호출 등 네트워크를 사용한 호출 시 사용
// 5. Schedulers.newThread() - 새로운 스레드를 만드는 스케쥴러
// 6. AndroidSchedulers.mainThread() - 안드로이드의 UI 스레드에서 동작
// 7. HandlerScheduler.from(handler) - 특정 핸들러 handler에 의존하여 동작
//
// RxAndroid에서 기본 스레드는 메인스레드에서 실행
//
// 참고 사이트 :
// http://tiii.tistory.com/18
// https://blog.realm.io/kr/realm-java-0.87.0
// https://github.com/dalinaum/writing/blob/master/rx-android1.md