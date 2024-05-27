package org.example;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


//OpenCV播放视频
public class VideoProcess implements Runnable {
    private String videoPath;
    private String audioPath;
    public BufferedImage image;

    //生产者——消费者缓存区
    private Mat[] ImageBuffer = new Mat[5];
    private volatile int ImageBufferUpIndex = 0;
    private volatile int ImageBufferDownIndex = 0;

    //播放结束标志
    public boolean isEnd = false;

    public VideoProcess(String videoPath, String audioPath) {
        this.videoPath = videoPath;
        this.audioPath = audioPath;
    }

    @Override
    public void run() {
        VideoCapture vc = new VideoCapture(videoPath);
        Mat frame = new Mat();
        double fps = vc.get(5);
        long startTime = System.currentTimeMillis();
        InGameKeyHandler.startTime = startTime; // 精同步

        //播放音频
        new Thread(() -> {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(audioPath));
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.open(audioStream);
                audioClip.start();
                new Thread(() -> {
                    //终止
                    while (!isEnd) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    audioClip.stop();
                    try {
                        audioStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        //播放视频
        new Thread(() -> {
            int cnt = 0;
            long offset = (long) (1000 / fps);
            boolean stopForOneFrame = false;
            while (true) {
                if (ImageBufferUpIndex != ImageBufferDownIndex) {
                    if(!stopForOneFrame){
                        image = mat2Image(ImageBuffer[ImageBufferDownIndex]);
                        ImageBufferDownIndex = (ImageBufferDownIndex + 1) % 5;
                        cnt++;
                    }
                    else{
                        stopForOneFrame = false;
                    }
                    try {
                        if (offset < 0) offset = 0;
                        Thread.sleep(offset);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //随时调整偏移量,使视频播放速度与音频播放速度一致
                    long curTime = System.currentTimeMillis();
                    double error = (double)cnt - fps * (curTime - startTime) / 1000;
                    //偏移过大，跳帧或顿帧
                    if(error < -1.0){
                        //播慢了，跳帧
                        if(ImageBufferUpIndex != ImageBufferDownIndex){
                            image = mat2Image(ImageBuffer[ImageBufferDownIndex]);
                            ImageBufferDownIndex = (ImageBufferDownIndex + 1) % 5;
                            cnt++;
                        }
                        offset--;
                    }
                    else if(error > 1.0){
                        //播快了，顿帧
                        stopForOneFrame = true;
                        offset++;
                    }
                    //调试
                    //System.out.println(offset);
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (isEnd) {
                    break;
                }
            }
        }).start();
        while (true) {
            //提前终止
            if (!InGameKeyHandler.active) {
                break;
            }

            //生产者消费者模型
            if ((ImageBufferUpIndex + 1) % 5 != ImageBufferDownIndex) {
                //生产Mat Frame
                if(!vc.read(frame)){
                    break;
                }
                if (frame.empty()) {
                    System.out.println("No captured frame -- Break!");
                    break;
                }
                ImageBuffer[ImageBufferUpIndex] = frame.clone();
                ImageBufferUpIndex = (ImageBufferUpIndex + 1) % 5;
            }
            else {
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //InGameKeyHandler.endComposing();
        //System.out.println("播放完毕");
        isEnd = true;
    }

    //Mat转换成BufferedImage
    private BufferedImage mat2Image(Mat frame) {

        final double photomask = 0.5;
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = new byte[frame.width() * frame.height() * (int) frame.elemSize()];
        int type;
        frame.get(0, 0, data);
        switch (frame.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i += 3) {
                    b = (byte) ((data[i] & 0xFF) * photomask);
                    data[i] = (byte) ((data[i + 2] & 0xFF) * photomask);
                    data[i + 1] = (byte) ((data[i + 1] & 0xFF) * photomask);
                    data[i + 2] = b;
                }
                break;
            default:
                return null;
        }

        image.getRaster().setDataElements(0, 0, frame.width(), frame.height(), data);
        return image;
    }
}

