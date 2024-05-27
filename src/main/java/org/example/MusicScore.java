package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;

enum NoteType {
    RED,
    BLUE
}

enum JudgeType {
    UNHIT,
    PERFECT,
    GOOD,
    MISS
}

// 谱面
public class MusicScore {
    int length = 1920;
    int height = 1080;

    // 谱面BPM
    int bpm = 170;

    // 谱面每小节时间
    long subsectionTime = 1000 * 60 / bpm * 4;
    public class Note {
        // Note类型
        NoteType type;
        JudgeType judgeType = JudgeType.UNHIT;

        // Hit时间,以毫秒计算
        long time;
        public Note(String type, long time) {
            if(type.equals("R")) {
                this.type = NoteType.RED;
            }
            else if(type.equals("B")) {
                this.type = NoteType.BLUE;
            }
            this.time = time;
        }
    }
    public ArrayList<Note> notes;
    public int activeNoteIndex = 0;
    boolean noteIsActive = false;
    public int firstNoteIndex = 0;
    public int lastNoteIndex = 0;
    public String title;
    public int totalCombo;
    public int totalScore;
    public MusicScore(String path){
        // 读取谱面，创建Note对象
        notes = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            while(true){
                String line = br.readLine();
                if(line == null){
                    break;
                }
                String[] split = line.split(" ");
                Note note = new Note(split[1], Long.parseLong(split[0]));
                notes.add(note);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        totalCombo = notes.size();
        totalScore = 300 * totalCombo;
        if(totalCombo > 0) {
            totalScore += (1+totalCombo) * totalCombo / 2;
        }

        //初步同步InGameKeyHandler的startTime，防止一开始出现MISS
        InGameKeyHandler.startTime = System.currentTimeMillis();
        //使用滑动窗口维护需要绘制的Note范围
        new Thread(()->{
            while(true){
                if(!InGameKeyHandler.active) break;
                long curTime = System.currentTimeMillis() - InGameKeyHandler.startTime;
                if(firstNoteIndex <= notes.size()-2){
                    if(curTime - notes.get(firstNoteIndex + 1).time > subsectionTime){
                        firstNoteIndex++;
                    }
                }
                //System.out.println(firstNoteIndex);
                if(lastNoteIndex <= notes.size()-2){
                    if(notes.get(lastNoteIndex + 1).time - curTime < subsectionTime * 2){
                        lastNoteIndex++;
                    }
                }
                //System.out.println(lastNoteIndex);

                //更新活动Note
                if(activeNoteIndex <= notes.size()-2){
                    if(curTime - notes.get(activeNoteIndex).time >
                            (notes.get(activeNoteIndex+1).time - notes.get(activeNoteIndex).time) /2
                            || notes.get(activeNoteIndex).judgeType != JudgeType.UNHIT
                    ) {
                        activeNoteIndex++;

                        //错过判定
                        if(notes.get(activeNoteIndex - 1).judgeType == JudgeType.UNHIT) {
                            notes.get(activeNoteIndex - 1).judgeType = JudgeType.MISS;
                            GameStatistics.addMiss();
                            GameStatistics.resetCombo();
                            setPaintJudge(JudgeType.MISS);
                        }
                    }
                }

                //判定活动Note是否被判定
                long offset = notes.get(activeNoteIndex).time - curTime;
                if(abs(offset) < 180){
                    synchronized (this){
                    noteIsActive = true;
                    }
                }
                else{
                    synchronized (this){
                        noteIsActive = false;
                    }

                    //错过判定
                    if(offset < 0) {
                        if (notes.get(activeNoteIndex).judgeType == JudgeType.UNHIT) {
                            notes.get(activeNoteIndex).judgeType = JudgeType.MISS;
                            GameStatistics.addMiss();
                            GameStatistics.resetCombo();
                            setPaintJudge(JudgeType.MISS);
                        }
                    }
                }
                try{
                    Thread.sleep(10);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        // 从路径中提取歌曲名
        Pattern pattern = Pattern.compile("(?<=\\\\).+(?=\\\\musicScore\\.txt$)");
        Matcher matcher = pattern.matcher(path);
        if(matcher.find()) {
            title = matcher.group();
        }
        else {
            title = "Unknown";
        }
    }

    //绘制Note
    public void paintNodes(Graphics g){
        try {
            BufferedImage redNote = ImageIO.read(new FileInputStream(GameRes.redNote));
            BufferedImage blueNote = ImageIO.read(new FileInputStream(GameRes.blueNote));
            if(notes == null) return;
            //反向遍历note
            for(int i = lastNoteIndex; i >= firstNoteIndex; i--) {
                Note note = notes.get(i);
                if (note.judgeType == JudgeType.PERFECT
                        || note.judgeType == JudgeType.GOOD) {

                    //绘制Note飞出
                    long curTime = System.currentTimeMillis() - InGameKeyHandler.startTime;
                    long offset = note.time - curTime;
                    int xPosition = (int) (length / 6 - (double)offset / 5);
                    int yPosition = (int) (height / 5 + 50 + 5 * (double) offset / 5);
                    if (!(xPosition > length || xPosition < 0)
                            && !(yPosition > height + 300 || yPosition < 0)) {
                        if (note.type == NoteType.RED) {
                            g.drawImage(redNote, xPosition,
                                    yPosition, height / 10, height / 10, null);
                        } else if (note.type == NoteType.BLUE) {
                            g.drawImage(blueNote, xPosition,
                                    yPosition, height / 10, height / 10, null);
                        }
                    }

                }
                else if (note.judgeType == JudgeType.UNHIT
                        ||note.judgeType == JudgeType.MISS) {

                    //绘制Note沿轨道移动
                    int xPosition = getXPosition(note);
                    if (!(xPosition > length || xPosition < 0)) {
                        if (note.type == NoteType.RED) {
                            g.drawImage(redNote, xPosition,
                                    height / 5 + 50, height / 10, height / 10, null);
                        } else if (note.type == NoteType.BLUE) {
                            g.drawImage(blueNote, xPosition,
                                    height / 5 + 50, height / 10, height / 10, null);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getXPosition(Note note) {
        long curTime = System.currentTimeMillis() - InGameKeyHandler.startTime;
        long offset = note.time - curTime;
        return (int) (length / 6 + (double)offset / (double)subsectionTime * length * 5 / 6);
    }

    //判定监听器
    public void preJudge(){
        new Thread(() ->{
            while(true) {
                if(!InGameKeyHandler.active) break;
                long curTime = System.currentTimeMillis() - InGameKeyHandler.startTime;
                if (    noteIsActive &&
                        !InGameKeyHandler.beatHandled &&
                        curTime > InGameKeyHandler.beatTime &&
                        curTime < InGameKeyHandler.beatAbortTime) {
                    synchronized (InGameKeyHandler.class) {
                        InGameKeyHandler.beatHandled = true;
                    }
                    judge(notes.get(activeNoteIndex), InGameKeyHandler.beatTime, InGameKeyHandler.hitType);
                }
                try {
                    Thread.sleep(2);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //绘制判定用变量
    public volatile boolean needPaintJudge = false;
    public volatile JudgeType judgeType;
    public volatile long judgeTime;

    //判定逻辑
    public void judge(Note note, long hitTime, InGameKeyHandler.HitType hitType){
        if(note.judgeType != JudgeType.UNHIT) return;
        if(!(note.type == NoteType.RED && hitType == InGameKeyHandler.HitType.RED
                || note.type == NoteType.BLUE && hitType == InGameKeyHandler.HitType.BLUE)) {
            note.judgeType = JudgeType.MISS;
            GameStatistics.addMiss();
            GameStatistics.resetCombo();
            setPaintJudge(JudgeType.MISS);
            return;
        }
        long offset = abs(note.time - hitTime);
        if(offset <= 120 && offset > 60) {
            note.judgeType = JudgeType.GOOD;
            GameStatistics.addGood();
            GameStatistics.addCombo();
            GameStatistics.addScore(100 + GameStatistics.combo);
            setPaintJudge(JudgeType.GOOD);
        }
        else if(offset <= 60) {
            note.judgeType = JudgeType.PERFECT;
            GameStatistics.addPerfect();
            GameStatistics.addCombo();
            GameStatistics.addScore(300 + GameStatistics.combo);
            setPaintJudge(JudgeType.PERFECT);
        }
        else {
            note.judgeType = JudgeType.MISS;
            GameStatistics.addMiss();
            GameStatistics.resetCombo();
            setPaintJudge(JudgeType.MISS);
        }
    }
    private void setPaintJudge(JudgeType judgeType){
        this.judgeType = judgeType;
        synchronized (this) {
            needPaintJudge = true;
        }
        judgeTime = System.currentTimeMillis();
    }
}
