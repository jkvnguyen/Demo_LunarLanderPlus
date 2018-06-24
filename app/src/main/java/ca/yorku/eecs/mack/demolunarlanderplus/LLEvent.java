package ca.yorku.eecs.mack.demolunarlanderplus;

/**
 *          DemoLunarLanderPlus - with modifications by ...
 *
 *          LoginID - nguye688
 *          StudentID - nguye688
 *          Last name - Nguyen
 *          First name - Jeremy
 */

public class LLEvent {

    public int level;
    public int score;
    public float avgLevel;
    public float avgLvlTime;
    public float avgFuelLeft;
    public int numOfTrials;

    public LLEvent(int level, int playerScore, float avgLevel, float avgLvlTime, float avgFuelLeft, int numOfTrials){
        this.level = level;
        this.score = playerScore;
        this.avgLevel = avgLevel;
        this.avgLvlTime = avgLvlTime;
        this.avgFuelLeft = avgFuelLeft;
        this.numOfTrials = numOfTrials;
    }
}
