package org.example;


//音游计分参数
public class GameStatistics {
    //分数
    public static int score;

    //连击数
    public static int combo;

    //最大连击数
    public static int maxCombo;

    //perfect判定数
    public static int perfect;

    //good判定数
    public static int good;

    //miss判定数
    public static int miss;

    //计分方法
    public static void resetGameStatistics() {
        score = 0;
        combo = 0;
        maxCombo = 0;
        perfect = 0;
        good = 0;
        miss = 0;
    }

    public static void addScore(int score) {
        GameStatistics.score += score;
    }

    public static void addCombo() {
        combo++;
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public static void resetCombo() {
        combo = 0;
    }

    public static void addPerfect() {
        perfect++;
    }


    public static void addGood() {
        good++;
    }

    public static void addMiss() {
        miss++;
    }
}
