package org.example;

import javax.swing.*;
import java.io.File;

public class Main {
    // 加载OpenCV库
    static {
        File f = new File("opencv\\opencv_java470.dll");
        File f2 = new File("opencv\\opencv_videoio_ffmpeg470_64.dll");
        System.load(f.getAbsolutePath());
        System.load(f2.getAbsolutePath());
    }

    public static void main(String[] args) {
        // 创建窗口
        SwingUtilities.invokeLater(()-> {
            TaikoFrame taikoFrame = new TaikoFrame();
        });
    }
}