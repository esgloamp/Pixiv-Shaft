package ceui.lisa.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import ceui.lisa.activities.Shaft;
import ceui.lisa.interfs.Callback;
import ceui.lisa.interfs.ListShow;
import ceui.lisa.response.IllustsBean;
import ceui.lisa.response.ListIllustResponse;
import ceui.lisa.response.UserModel;
import ceui.lisa.utils.Empty;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Local {

    public static final String LOCAL_DATA = "local_data";
    public static final String USER = "user";

    public static void saveUser(UserModel userModel){
        if(userModel != null){
            Gson gson = new Gson();
            String userString = gson.toJson(userModel, UserModel.class);
            SharedPreferences localData = Shaft.getContext().getSharedPreferences(LOCAL_DATA, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = localData.edit();
            editor.putString(USER, userString);
            editor.apply();
        }
    }

    public static UserModel getUser(){
        SharedPreferences localData = Shaft.getContext().getSharedPreferences(LOCAL_DATA, Context.MODE_PRIVATE);
        String userString = localData.getString(USER,"");
        Gson gson = new Gson();
        UserModel userModel = gson.fromJson(userString, UserModel.class);
        return userModel;
    }


    /**
     * 主线程 同步写入本地文件
     *
     * @param t
     * @param <T>
     */
    public static <T> void saveIllustList(List<T> t){
        try {
            FileOutputStream outputStream = Shaft.getContext().openFileOutput("RecommendIllust", Activity.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(t);//写入
            outputStream.close();//关闭输入流
            oos.close();
            Common.showLog("本地文件写入成功");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * IO线程 异步写入本地文件
     *
     * @param t
     * @param index
     * @param <T>
     */
    public static <T> void saveIllustList(List<T> t, int index) {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onNext("开始写入本地文件");
                FileOutputStream outputStream = Shaft.getContext().openFileOutput("RecommendIllust", Activity.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(t);//写入
                outputStream.close();//关闭输入流
                oos.close();
                emitter.onNext("本地文件写入完成");
                emitter.onComplete();
            }
        });
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        Common.showLog(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Common.showToast(e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }





    /**
     * 管道
     *
     * @param <Target>
     */
    public static class IllustPip<Target>{

        private List<Target> beans = new ArrayList<>();

        List<Target> getBeans() {
            return beans;
        }

        void setBeans(List<Target> beans) {
            this.beans = beans;
        }
    }


    /**
     * 主线程 同步读取本地文件
     *
     * @param <T>
     * @return
     */
    public static <T> List<T> getLocalIllust() {
        List<T> bean = new ArrayList<>();
        try {
            Common.showLog("getLocalIllust thread is : " + Thread.currentThread().getName());
            FileInputStream fis = Shaft.getContext().openFileInput("RecommendIllust");//获得输入流
            ObjectInputStream ois = new ObjectInputStream(fis);
            bean = (List<T>) ois.readObject();
            fis.close();
            ois.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return bean;
    }


    /**
     * IO线程 异步读取本地文件
     *
     * @param callback
     * @param <T>
     */
    public static <T> void getLocalIllust(Callback<List<T>> callback) {
        IllustPip<T> pip = new IllustPip<>();
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            emitter.onNext("开始读取本地文件");
            Common.showLog("Observable thread is : " + Thread.currentThread().getName());
            FileInputStream fis = Shaft.getContext().openFileInput("RecommendIllust");//获得输入流
            ObjectInputStream ois = new ObjectInputStream(fis);
            pip.setBeans((List<T>) ois.readObject());
            fis.close();
            ois.close();
            emitter.onNext("本地文件读取完成");
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        Common.showLog(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Common.showToast(e.toString());
                    }

                    @Override
                    public void onComplete() {
                        callback.doSomething(pip.getBeans());
                    }
                });
    }
}
