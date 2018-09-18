package com.google.android.cameraview.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.heaven7.java.base.util.Logger;
import com.heaven7.java.base.util.Predicates;
import com.heaven7.java.base.util.threadpool.Executors2;
import com.heaven7.java.visitor.FireVisitor;
import com.heaven7.java.visitor.StartEndVisitor;
import com.heaven7.java.visitor.collection.VisitServices;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceRec;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by heaven7 on 2018/9/18 0018.
 */
public class FaceDetectActivity extends AppCompatActivity {

    private static final String TAG = "FaceDetectActivity";
    private static final String VIDEO_PATH = Environment.getExternalStorageDirectory() +"/dinner_C0074.mp4";
    private ExecutorService mService = Executors2.newFixedThreadPool(1);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getFaceForVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService != null){
            mService.shutdownNow();
        }
    }

    private void getFaceForVideo() {
        MediaPlayer player = new MediaPlayer();
        final Uri uri = Uri.fromFile(new File(VIDEO_PATH));
        final int duration;
        try {
            player.setDataSource(getApplicationContext(), uri);
            player.prepare();
            duration = player.getDuration();
            player.release();
            System.out.println("duration = " + duration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final FaceRec faceRec = new FaceRec(Constants.getDLibDirectoryPath());
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(getApplicationContext(), uri);
                int max = duration / 1000;
                int index = 0;
                final StringBuilder sb = new StringBuilder();
                while (index <= max){
                    final Bitmap bitmap = retriever.getFrameAtTime(index * 1000 * 1000);
                    if(bitmap == null){
                        break;
                    }
                    sb.append(index);
                    List<VisionDetRet> rets = faceRec.detect(bitmap);
                    Log.d(TAG ,"read frame success, index = " + index);
                    //0,0.1 0.1 0.1 0.1
                   // if(rets != null &&)
                    if(!Predicates.isEmpty(rets)){
                        sb.append(",");
                        VisitServices.from(rets).fireWithStartEnd(new StartEndVisitor<VisionDetRet>() {
                            @Override
                            public boolean visit(Object param, VisionDetRet vdt, boolean start, boolean end) {
                                sb.append(vdt.getLeft() * 1f / bitmap.getWidth()).append(" ")
                                        .append(vdt.getTop() * 1f / bitmap.getHeight()).append(" ")
                                        .append(vdt.getWidth() * 1f / bitmap.getWidth()).append(" ")
                                        .append(vdt.getHeight() * 1f / bitmap.getHeight());
                                if(!end){
                                    sb.append(" ");
                                }
                                return false;
                            }
                        });
                        sb.append("\n");
                    }
                    index += 1;
                }
                retriever.release();
                Log.d(TAG ,sb.toString());
            }
        };
        mService.submit(r);
    }
}
