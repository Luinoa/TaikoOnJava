package org.example;

import javax.sound.sampled.*;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


public class InGameKeyHandler {
    //谱面路径
    //写谱用
    //public static String path;
    //static BufferedWriter bw = null;

    //是否处理按键事件
    public static boolean active = false;

    //开始时间
    public static long startTime = 0;

    //鼓面是否亮起
    public static volatile boolean dHit = false;
    public static volatile boolean fHit = false;
    public static volatile boolean jHit = false;
    public static volatile boolean kHit = false;


    //设置判定
    enum HitType {
        RED,
        BLUE
    }
    public volatile static long beatTime = -1;
    public volatile static long beatAbortTime = -1;
    public volatile static HitType hitType = null;
    public volatile static boolean beatHandled = true;

    public static void handleKeyPress(int key) {
        switch (key) {
            //DFJK
            case 68:
                dHit = true;
                Decide.hitBlue();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        dHit = false;
                    }
                }, 100);
                break;
            case 70:
                fHit = true;
                Decide.hitRed();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        fHit = false;
                    }
                }, 100);
                break;
            case 74:
                jHit = true;
                Decide.hitRed();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        jHit = false;
                    }
                }, 100);
                break;
            case 75:
                Decide.hitBlue();
                kHit = true;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        kHit = false;
                    }
                }, 100);
                break;

            //ESC，主动停止
            case 27:
                active = false;
                break;
        }
    }

    //重设判定时间
    public static void resetDecide() {
        synchronized (InGameKeyHandler.class) {
            beatTime = -1;
            beatAbortTime = -1;
            beatHandled = true;
        }
    }
    /*
     *  写谱用
    public static void endComposing(){
        if(null != bw){
            try{
                bw.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
     */

    //按键，设置判定
    private static class Decide {
        public static void hitRed() {
            playDrumBeat(redDrumBeat);
            synchronized (InGameKeyHandler.class) {
                beatTime = System.currentTimeMillis() - startTime;
                beatAbortTime = beatTime + 10;
                hitType = HitType.RED;
                beatHandled = false;
            }
        }

        public static void hitBlue() {
            playDrumBeat(blueDrumbeat);
            synchronized (InGameKeyHandler.class) {
                beatTime = System.currentTimeMillis() - startTime;
                beatAbortTime = beatTime + 10;
                hitType = HitType.BLUE;
                beatHandled = false;
            }
        }
        /*
         * 写谱用
        public static void createBW(){
            try{
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path))));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        public static void hitRed(){
            if(null != bw){
                try{
                    bw.write(String.valueOf(System.currentTimeMillis() - startTime) + " " + "R");
                    bw.newLine();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            else{
                createBW();
                hitRed();
            }
        }
        public static void hitBlue(){
            if(null != bw){
                try{
                    bw.write(String.valueOf(System.currentTimeMillis() - startTime) + " " + "B");
                    bw.newLine();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            else{
                createBW();
                hitBlue();
            }
        }
         */
    }
    //加载鼓声
    private static File redDrumBeat = new File(GameRes.redHit);
    private static File blueDrumbeat = new File(GameRes.blueHit);
    //播放鼓声
    private static void playDrumBeat(File drumBeat) {
        new Thread(() -> {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(drumBeat);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.open(audioStream);
                audioClip.start();
                new Thread(() -> {
                    try {
                        Thread.sleep(400);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    audioClip.stop();
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

