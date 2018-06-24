package ca.yorku.eecs.mack.demolunarlanderplus;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import ca.yorku.eecs.mack.demolunarlanderplus.LunarView.LunarThread;

/**
 *          DemoLunarLanderPlus - with modifications by ...
 *
 *          LoginID - nguye688
 *          StudentID - nguye688
 *          Last name - Nguyen
 *          First name - Jeremy
 */

@SuppressWarnings("unused")
public class DemoLunarLanderPlusActivity extends Activity implements View.OnTouchListener, LunarView.OnGameEndListener
{
    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    File f1, f2;
    BufferedWriter sd1, sd2, sd3;
    final String APP = "LunarLander";
    final String WORKING_DIRECTORY = "/LunarLanderProjectData/";
    String sd2Leader;
    final String SD2_HEADER = "App,Participant,Block,Group," +
            "Level,Score,AverageLevel,AverageTime,AverageFuel,NumOfTrials\n";

    boolean showScoreboardSet;
    String participantCode;
    boolean isTopFive;
    HashMap<String, Integer> scores = new HashMap<String, Integer>();

    private static final int MENU_PAUSE = 4;
    private static final int MENU_RESUME = 5;
    private static final int MENU_START = 6;
    private static final int MENU_STOP = 7;
    private static final int MENU_SCOREBOARD = 8;
    private static final int MENU_EXIT = 9;
    final int MAX_TOUCH_POINTS = 10;
    final int INVALID = -1;
    /**
     * Variables to manage multitouch event pointers.
     */
    int index, id;
    int[] touchPointId = new int[MAX_TOUCH_POINTS];
    int[] buttonId = new int[MAX_TOUCH_POINTS];
    /**
     * A handle to the thread that's actually running the animation.
     */
    private LunarThread lunarThread;
    /**
     * A handle to the View in which the game is running.
     */
    private LunarView lunarView;
    /**
     * A button pad for user input.
     */
    private ButtonPad buttonPad;

    /**
     * Invoked when the Activity is created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Log.i(MYDEBUG, "onCreate! saveInstanceState=" + savedInstanceState);

        //Scores recorded as part of the initial participants
        scores.put("P02", 136);
        scores.put("P07", 151);
        scores.put("P01", 129);
        scores.put("P04", 238);
        scores.put("P05", 355);

        scores = sortHashMapByValues(scores);

        Log.i(MYDEBUG, scores.toString());

        // use the layout defined in the XML file
        setContentView(R.layout.lunar_layout);

        // get the button pad from the XML (used for player input)
        buttonPad = (ButtonPad)findViewById(R.id.buttonSet);

        // attached a touch listener to the button pad
        buttonPad.setOnTouchListener(this);

        // get a handle to the LunarView from XML
        lunarView = (LunarView)findViewById(R.id.lunar);

        //show only actionBar
        lunarView.setSystemUiVisibility(4);

        lunarView.setOnGameEndListener(this);


        // get the TextView from the XML and pass a reference to the LunarView (used for messages)
        lunarView.setTextView((TextView)findViewById(R.id.text));

        // give the LunarThread a vibrator object (used when the engine is firing)
        Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        lunarView.setVibrator(v);

        // invalidate all touch point ids and button ids
        for (int i = 0; i < touchPointId.length; ++i)
        {
            touchPointId[i] = INVALID;
            buttonId[i] = INVALID;
        }

        // The launcher icon has "LL+" as the title. Provide the full title in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setTitle("Lunar Lander Plus");

        Bundle b = getIntent().getExtras();
        participantCode = b.getString("participantCode");
        String groupCode = b.getString("groupCode");
        showScoreboardSet = b.getBoolean("showScoreboardSet");

        File dataDirectory = new File(Environment.getExternalStorageDirectory() + WORKING_DIRECTORY);
        if (!dataDirectory.exists() && !dataDirectory.mkdirs())
        {
            Log.e(MYDEBUG, "ERROR --> FAILED TO CREATE DIRECTORY: " + WORKING_DIRECTORY);
            super.onDestroy(); // cleanup
            this.finish(); // terminate
        }
        Log.i(MYDEBUG, "Working directory=" + dataDirectory);

        int blockNumber = 0;
        do
        {
            ++blockNumber;
            String blockCode = String.format(Locale.CANADA, "B%02d", blockNumber);
            String baseFilename = String.format("%s-%s-%s", participantCode,
                    blockCode, groupCode);

            f2 = new File(dataDirectory, baseFilename + ".sd2");

            sd2Leader = String.format("%s,%s,%s,%s", APP, participantCode,
                    blockCode, groupCode);
        } while (f2.exists());

        try
        {
            sd2 = new BufferedWriter(new FileWriter(f2));

            // output header in sd2 file
            sd2.write(SD2_HEADER, 0, SD2_HEADER.length());
            sd2.write(sd2Leader);
            sd2.flush();
        } catch (IOException e)
        {
            Log.e(MYDEBUG, "ERROR OPENING DATA FILES! e=" + e.toString());
            super.onDestroy();
            this.finish();
        } // end file initialization

    }

    /*
     * Wait until the activity window has focus to get the lunar thread (because the lunar thread is
     * created in the lunar view's surfaceCreated method).
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (hasFocus)
        {
            // Log.i(MYDEBUG, "onWindowFocusChanged (has focus)!");
            lunarThread = lunarView.getThread();
        }
    }

    // invoked during init to give the Activity a chance to set up its Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        //determine whether scoreboard on or not
        if(showScoreboardSet){
            menu.add(0, MENU_SCOREBOARD, 0, "Scoreboard");
        }
        menu.add(0, MENU_EXIT, 0, "Exit");
        return true;
    }

    // invoked when the user selects an item from the Menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_START:
                lunarThread.doStart();
                return true;
            case MENU_STOP:
                lunarThread.setState(LunarThread.STATE_LOSE, getText(R.string.message_stopped));
                return true;
            case MENU_PAUSE:
                lunarThread.doPause();
                return true;
            case MENU_RESUME:
                lunarThread.doUnpause();
                return true;
            case MENU_SCOREBOARD:
                showResultsDialog("Leaderboard\n\n" + showLeaderboard());
                return true;
            case MENU_EXIT:
                // Exit the app. See, also, the run method in LunarThread.
                lunarThread.interrupt();
                onDestroy();
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent me)
    {
        lunarView.setSystemUiVisibility(4);
        // Log.i(MYDEBUG, "Activity onTouch!");
        // get the index of the pointer associated with this event
        index = me.getActionIndex();

        // get the id of the pointer associated with this event
        id = me.getPointerId(index);

        // identify the ButtonPad button associated with this touch event
        final int tmpButton = buttonPad.getButton(me.getX(index), me.getY(index));

        // process the event
        switch (me.getAction() & MotionEvent.ACTION_MASK)
        {
            // touch down (first finger or subsequent finger)
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                // find an empty spot in the arrays for the new touch point
                for (int i = 0; i < MAX_TOUCH_POINTS; ++i)
                {
                    if (touchPointId[i] == INVALID)
                    {
                        touchPointId[i] = id;
                        buttonId[i] = tmpButton;
                        break;
                    }
                }

                // tell the button pad which key was pressed (updates L&F)
                buttonPad.setPressed(tmpButton);

                // let the thread work its magic (update game state and physics, draw the game)
                lunarThread.doKeyDown(tmpButton);

				/*
				 * If down-event is from the Down button, lunarThread is interrupted (except during
				 * game play). In this case, we're done.
				 */
                if (lunarThread.isInterrupted())
                {
                    onDestroy();
                    finish();
                }
                break;

            case MotionEvent.ACTION_UP: // last touch point
            case MotionEvent.ACTION_POINTER_UP:
                // find the released touch point, release the button, make it invalid, draw and update game via thread
                for (int i = 0; i < MAX_TOUCH_POINTS; ++i)
                {
                    if (touchPointId[i] == id)
                    {
                        touchPointId[i] = INVALID;
                        buttonPad.setReleased(buttonId[i]);
                        buttonId[i] = INVALID;
                        lunarThread.doKeyUp(tmpButton);
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                break;
        }
        return true;
    }

    // invoked when the Activity loses user focus.
    @Override
    protected void onPause()
    {
        // Log.i(MYDEBUG, "onPause!");
        super.onPause();
        lunarView.getThread().doPause(); // pause game when Activity pauses
    }

    @Override
    protected void onStart()
    {
        // Log.i(MYDEBUG, "onStart!");
        super.onStart();
    }

    @Override
    protected void onResume()
    {
        // Log.i(MYDEBUG, "onResume!");
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        // Log.i(MYDEBUG, "onStop!");
        super.onStop();
        MyApplication ma = (MyApplication)getApplicationContext();
        Bundle b = lunarThread.saveState(new Bundle());
        ma.setBundle(b);
    }

    @Override
    protected void onDestroy()
    {
        // Log.i(MYDEBUG, "onDestroy!");
        super.onDestroy();
    }

    @Override
    protected void onRestart()
    {
        // Log.i(MYDEBUG, "onRestart!");
        super.onRestart();
        MyApplication ma = (MyApplication)getApplicationContext();
        Bundle b = ma.getBundle();
        lunarView.setRestoreBundle(b);
    }

    public void onGameEnd(LLEvent le){

        checkScore(participantCode, le.score);
        final StringBuilder sd2Data = new StringBuilder(100);

        final StringBuffer sb = new StringBuffer("Your ...\n\nLevel: " + le.level + "\n");
        sb.append("Score: " + le.score + "\n");
        sb.append("Average level: " + String.format("%.2f", le.avgLevel) + "\n");
        sb.append("Average time: " + String.format("%.2f", le.avgLvlTime) + "\n");
        sb.append("Average fuel remaining: " + String.format("%.2f", le.avgFuelLeft) + "\n");

        sd2Data.append(String.format("%s, %s, %.2f, %.2f, %.2f, %s", le.level, le.score, le.avgLevel, le.avgLvlTime, le.avgFuelLeft,
                le.numOfTrials));

        try
        {
            sd2.write(sd2Data.toString(), 0, sd2Data.length());
            sd2.flush();
        } catch (IOException e)
        {
            Log.e("MYDEBUG", "ERROR WRITING TO DATA FILE!\n" + e);
            super.onDestroy();
            this.finish();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showEndDialog(sb.toString());
                //show leaderboards after game is over
                String temp = "";
                if(isTopFive){
                    temp = "You (" + participantCode +  ") got a high score!\n\n";
                }
                showResultsDialog(temp + showLeaderboard());
            }
        });

    }

    private void showResultsDialog(String text)
    {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.results_dialog, (ViewGroup)findViewById(R.id.results_layout));

        // Set text
        TextView results = (TextView)layout.findViewById(R.id.resultsArea);
        results.setText(text);

        // Initialize the dialog
        AlertDialog.Builder parameters = new AlertDialog.Builder(this);
        parameters.setView(layout).setCancelable(false).setNeutralButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel(); // close this dialog
            }
        }).show();
    }

    //Dialog for end of game, added finish()
    private void showEndDialog(String text)
    {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.results_dialog, (ViewGroup)findViewById(R.id.results_layout));

        // Set text
        TextView results = (TextView)layout.findViewById(R.id.resultsArea);
        results.setText(text);

        // Initialize the dialog
        AlertDialog.Builder parameters = new AlertDialog.Builder(this);
        parameters.setView(layout).setCancelable(false).setNeutralButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel(); // close this dialog
                finish();
            }
        }).show();
    }

    //check score if there is a highscore to be changed
    private void checkScore(String participant, int score){

        isTopFive = false;

        for(int top5Score : scores.values()){
            if(score > top5Score){
                isTopFive = true;
            }
        }

        if(isTopFive){
            scores.put(participant, score);
            scores = sortHashMapByValues(scores);

            //remove first element (smallest element)
            List<String> mapKeys = new ArrayList<String>(scores.keySet());
            scores.remove(mapKeys.get(0));
        }


    }

    public String showLeaderboard(){
        String temp = "";
        int count = 1;
        List<String> mapKeys = new ArrayList<String>(scores.keySet());
        List<Integer> mapValues = new ArrayList<Integer>(scores.values());
        for(int i = scores.size()-1; i >= 0; i--){
            temp = temp + (count) + ".\t" + mapKeys.get(i) + "\t" + mapValues.get(i) + "\n";
            count++;
        }
        return temp;
    }

    //Code via 'https://stackoverflow.com/questions/8119366/sorting-hashmap-by-values' to sort a values in map without
    //affecting keys
    public LinkedHashMap<String, Integer> sortHashMapByValues(
            HashMap<String, Integer> passedMap) {
        List<String> mapKeys = new ArrayList<String>(passedMap.keySet());
        List<Integer> mapValues = new ArrayList<Integer>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<String, Integer> sortedMap =
                new LinkedHashMap<String, Integer>();

        Iterator<Integer> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            int val = valueIt.next();
            Iterator<String> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                int comp1 = passedMap.get(key);
                int comp2 = val;

                if (comp1 == comp2) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }

}
