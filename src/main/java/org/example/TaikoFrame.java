package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.util.HashSet;

public class TaikoFrame extends JFrame {
    private int length = 1920;
    private int height = 1080;
    private HashSet<Integer> keySet = new HashSet<>();//按键集合
    private int fps = 0;
    private long lastTime;
    public GameStatistics gameStatistics;
    public MusicScore curMusicScore;

    public TaikoFrame() {
        //创建 JFrame 对象
        setTitle("JTaiko");
        setSize(length, height);  //大小
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  //关闭窗口时的操作
        setLocationRelativeTo(null);   //窗口居中
        setResizable(false);   //窗口大小不可变

        //创建开始界面
        JPanel titlePanel = new JPanel();
        titlePanel.setBounds(length / 4, height / 4, length / 2, height / 2);
        titlePanel.add(new JLabel(new ImageIcon(GameRes.title)));
        titlePanel.setBackground(Color.BLACK);

        //创建按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.setLayout(new GridLayout(2, 1, 0, 20));
        buttonPanel.setBounds(length * 4 / 5, height / 2, 200, 120);

        //创建按钮
        JButton startButton = new JButton("Start");
        startButton.setBackground(Color.ORANGE);
        startButton.setForeground(Color.CYAN);

        //创建键盘监听器
        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (keySet.contains(e.getKeyCode()))
                    return;
                //System.out.println(e.getKeyCode() + "keyTyped");
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (keySet.contains(e.getKeyCode()))
                    return;
                keySet.add(e.getKeyCode());
                //System.out.println(e.getKeyCode() + "keyPressed");
                if (InGameKeyHandler.active) {
                    InGameKeyHandler.handleKeyPress(e.getKeyCode());// 游戏中，按键传入处理器
                }
                else if(e.getKeyCode() == 27){
                    System.exit(0); // 不在游戏中，按Esc退出。
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!keySet.contains(e.getKeyCode()))
                    return;
                keySet.remove(e.getKeyCode());
                //System.out.println(e.getKeyCode() + "keyReleased");
            }
        };

        addKeyListener(keyListener);
        setFocusable(true);

        //添加按钮监听器
        startButton.addActionListener(e -> {
            //移除标题面板
            remove(titlePanel);
            remove(buttonPanel);
            //游戏开始
            inGame();
        });
        buttonPanel.add(startButton);
        JButton exitButton = new JButton("Exit");
        exitButton.setBackground(Color.CYAN);
        exitButton.setForeground(Color.ORANGE);
        exitButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(exitButton);
        add(buttonPanel);


        //显示开始界面
        add(titlePanel);
        setVisible(true);
    }

    public void inGame() {
        //初始化游戏类
        InGameKeyHandler.active = true;
        GameStatistics.resetGameStatistics();
        String musicScorePath = "musicScores\\GRANDIR\\";
        VideoProcess videoProcess = new VideoProcess
                (musicScorePath + "bga.mp4", musicScorePath + "audio.wav");
        InGameKeyHandler.resetDecide();
        //写谱用
        //InGameKeyHandler.path = musicScorePath + "musicScore.txt";

        //读取谱面
        curMusicScore = new MusicScore(musicScorePath + "musicScore.txt");

        //初始化帧率计时器
        lastTime = System.currentTimeMillis();

        //创建游戏面板
        JPanel gamePanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                try {
                    //绘制背景
                    g.drawImage(videoProcess.image, 0, 0, length, height, null);

                    //造成性能不足，注释掉，改为直接处理视频
                    //绘制遮罩
                    //FileInputStream afphotomask = new FileInputStream(GameRes.photomask);
                    //g.drawImage(ImageIO.read(photomask), 0, 0, length, height, null);

                    //绘制轨道
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, height / 5, length, height / 5);

                    //绘制Note
                    curMusicScore.paintNodes(g);

                    //绘制鼓面、判定圈
                    g.setColor(Color.BLACK);
                    g.fillRect(0, height / 5, height / 4 + 30, height / 5);
                    FileInputStream drum = new FileInputStream(GameRes.drum);
                    FileInputStream hitCircle = new FileInputStream(GameRes.hitCircle);
                    g.drawImage(ImageIO.read(drum),
                            length / 25, height / 5 + 5, null);
                    g.drawImage(ImageIO.read(hitCircle),
                            length / 6, height / 5 + 50, height / 10, height / 10, null);

                    //绘制判定
                    if(curMusicScore.needPaintJudge){
                        long curTime = System.currentTimeMillis() - curMusicScore.judgeTime;
                        if(curTime < 300){
                            g.setFont(new Font("Arial", Font.BOLD, 50));
                            if(curMusicScore.judgeType == JudgeType.PERFECT){
                                g.setColor(Color.YELLOW);
                                g.drawString("PERFECT", length / 6 - 50, height / 3 + 50 + (int) curTime / 10);
                            }
                            else if(curMusicScore.judgeType == JudgeType.GOOD){
                                g.setColor(Color.CYAN);
                                g.drawString("GOOD", length / 6 - 20, height / 3 + 50 + (int) curTime / 10);
                            }
                            else if(curMusicScore.judgeType == JudgeType.MISS){
                                g.setColor(Color.RED);
                                g.drawString("MISS", length / 6 - 5, height / 3 + 50 + (int) curTime / 10);
                            }
                        }
                        else {
                            synchronized (curMusicScore) {
                                curMusicScore.needPaintJudge = false;
                            }
                        }
                    }

//                  //绘制分数
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, length, 100);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 30));
                    g.drawString("COMBO", length / 2 - 100, 30);
                    g.drawString("SCORE", length - 200, 30);
                    g.setFont(new Font("Arial", Font.BOLD, 50));
                    g.drawString(String.valueOf(gameStatistics.score), length - 205, 80);
                    g.drawString(String.valueOf(gameStatistics.combo), length / 2 - 105, 80);

                    //绘制Esc提示
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.drawString("Press \"Esc\" to exit halfway.", 10, 30);

                    //绘制游戏中的鼓面亮起
                    FileInputStream drumFHit = new FileInputStream(GameRes.drumFHit);
                    FileInputStream drumJHit = new FileInputStream(GameRes.drumJHit);
                    FileInputStream drumDHit = new FileInputStream(GameRes.drumDHit);
                    FileInputStream drumKHit = new FileInputStream(GameRes.drumKHit);
                    if (InGameKeyHandler.fHit) {
                        g.drawImage(ImageIO.read(drumFHit), length / 25, height / 5 + 5, null);
                    }
                    if (InGameKeyHandler.jHit) {
                        g.drawImage(ImageIO.read(drumJHit), length / 25, height / 5 + 5, null);
                    }
                    if (InGameKeyHandler.dHit) {
                        g.drawImage(ImageIO.read(drumDHit), length / 25, height / 5 + 5, null);
                    }
                    if (InGameKeyHandler.kHit) {
                        g.drawImage(ImageIO.read(drumKHit), length / 25, height / 5 + 5, null);
                    }

                    //绘制帧率
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 30));
                    g.drawString(String.valueOf(fps) + " FPS", 1700, 1000);
                    g.setFont(new Font("Arial", Font.BOLD, 19));
                    if (fps > 120) {
                        g.setColor(Color.GREEN);
                        g.drawString("EXCELLENT PERFORMANCE", 1630, 1030);
                    } else if (fps > 60) {
                        g.setColor(Color.YELLOW);
                        g.drawString("SUFFICIENT PERFORMANCE", 1620, 1030);
                    } else if (fps > 20) {
                        g.setColor(Color.ORANGE);
                        g.drawString("PLAYABLE PERFORMANCE", 1640, 1030);
                    } else {
                        g.setColor(Color.RED);
                        g.drawString("SERIOUSLY?", 1700, 1030);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long curTime = System.currentTimeMillis();
                long timePassed = curTime - lastTime;
                if(timePassed != 0)
                    fps = (int) (1000 / timePassed);
                lastTime = System.currentTimeMillis();
            }
        };

        //播放视频
        InGameKeyHandler.startTime = System.currentTimeMillis(); // 粗同步
        new Thread(videoProcess).start();

        gamePanel.setBounds(0, 0, length, height);
        gamePanel.setBackground(Color.BLACK);
        add(gamePanel);

        //开启判定器
        curMusicScore.preJudge();

        new Thread(() -> {
            while (true) {
                if(videoProcess.isEnd) {
                    InGameKeyHandler.active = false;
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                gamePanel.repaint();
            }
            JPanel endPanel = new JPanel(){
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, length, height);
                    try {
                        FileInputStream jacket = new FileInputStream(musicScorePath + "jacket.png");
                        g.drawImage(ImageIO.read(jacket),
                                length / 10 , height / 6, 400, 400, null);
                        jacket.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    //绘制分数
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 50));
                    g.drawString(curMusicScore.title, length / 10, 100);
                    g.drawString("SCORE / TOTAL SCORE", length * 3 / 7, 50);
                    g.setColor(Color.ORANGE);
                    g.setFont(new Font("Arial", Font.BOLD, 100));
                    g.drawString(
                            String.valueOf(gameStatistics.score) + " / " + String.valueOf(curMusicScore.totalScore),
                            length * 3 / 7 + 100, height / 10 + 30);
                    g.setFont(new Font("Arial", Font.BOLD, 50));
                    g.setColor(Color.WHITE);
                    g.drawString("COMBO / FULL COMBO", length / 2 - 100, height / 5 + 50);
                    g.drawString(
                            String.valueOf(gameStatistics.maxCombo)+ " / " + String.valueOf(curMusicScore.totalCombo),
                            length / 2 - 100, height / 5 + 100);
                    g.setColor(Color.YELLOW);
                    g.drawString("PERFECT", length / 2 - 100, height / 5 + 200);
                    g.drawString(String.valueOf(gameStatistics.perfect), length / 2 - 100, height / 5 + 250);
                    g.setColor(Color.CYAN);
                    g.drawString("GOOD", length / 2 - 100, height / 5 + 350);
                    g.drawString(String.valueOf(gameStatistics.good), length / 2 - 100, height / 5 + 400);
                    g.setColor(Color.RED);
                    g.drawString("MISS", length / 2 - 100, height / 5 + 500);
                    g.drawString(String.valueOf(gameStatistics.miss), length / 2 - 100, height / 5 + 550);

                    //绘制Esc提示
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 40));
                    g.drawString("Press \"Esc\" to exit.", length / 10, height / 5 * 3);
                }
            };
            endPanel.setBounds(0, 0, length, height);
            remove(gamePanel);
            add(endPanel);
            endPanel.repaint();
        }).start();
    }
}
