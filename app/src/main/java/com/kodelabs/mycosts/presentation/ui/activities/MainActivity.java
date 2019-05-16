package com.kodelabs.mycosts.presentation.ui.activities;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;

import com.kodelabs.mycosts.R;
import com.kodelabs.mycosts.domain.executor.impl.ThreadExecutor;
import com.kodelabs.mycosts.domain.model.Cost;
import com.kodelabs.mycosts.presentation.animation.AnimatorFactory;
import com.kodelabs.mycosts.presentation.model.DailyTotalCost;
import com.kodelabs.mycosts.presentation.presenters.MainPresenter;
import com.kodelabs.mycosts.presentation.presenters.impl.MainPresenterImpl;
import com.kodelabs.mycosts.presentation.ui.adapters.CostItemAdapter;
import com.kodelabs.mycosts.storage.CostRepositoryImpl;
import com.kodelabs.mycosts.sync.auth.DummyAccountProvider;
import com.kodelabs.mycosts.threading.MainThreadImpl;
import com.kodelabs.mycosts.presentation.ui.activities.Mp3Recorder;
import com.kodelabs.mycosts.utils.Permission;
import com.kodelabs.mycosts.interfaces.Constant;

import java.util.List;
import java.util.Calendar;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.codetail.widget.RevealFrameLayout;
import timber.log.Timber;



public class MainActivity extends AppCompatActivity implements MainPresenter.View, Constant {//

    public static final String EXTRA_COST_ID = "extra_cost_id_key";
    private static final String TAG = "MainActivity";

    public static final int EDIT_COST_REQUEST = 0;
    public static boolean isWatching = false;
    public static boolean isRecording = false;
    public static int intDelayMillis = 200;
    public static boolean allowRECORD_AUDIO = false;

    //private static class DelayHandler extends Handler {}
    //private final DelayHandler mHandler = new DelayHandler();


    @Bind(R.id.expenses_list)
    RecyclerView mRecyclerView;

    @Bind(R.id.reveal_layout)
    RevealFrameLayout mRevealLayout;

    @Bind(R.id.button_start_watching)
    Button mButtonStartWatching;

    @Bind(R.id.watching_text_view)
    TextView mWatchingTextView;

    private MainPresenter mMainPresenter;

    private CostItemAdapter mAdapter;
    private static Mp3Recorder recorder = new Mp3Recorder();
    //Mp3Recorder recorder = new Mp3Recorder();


    private static class MyHandler extends Handler {}
    private final MyHandler mHandler = new MyHandler();

    public static class MyRunnable implements Runnable {
        private final WeakReference<Activity> mActivity;
        //private final WeakReference<Mp3Recorder> mRecorder;
        //private final WeakReference<AudioRecognizerWindow> mAudioRecognizer;


        public MyRunnable(Activity activity) {//, Mp3Recorder recorder , AudioRecognizerWindow audioRecognizer
            mActivity = new WeakReference<>(activity);
            //mRecorder = new WeakReference<>(recorder);
            //mAudioRecognizer = new WeakReference<>(audioRecognizer);
        }

        @Override
        public void run() {
            Log.i(TAG,  "run");

            Activity activity = mActivity.get();

            Calendar rightNow = Calendar.getInstance();
            //AudioRecognizerWindow audioRecognizer = mAudioRecognizer.get();
            if (activity != null) {
                TextView mWatchingTextView = (TextView) activity.findViewById(R.id.watching_text_view);
                //mWatchingTextView.setText("stop");

                if(isRecording) {
                    try {
                        recorder.stopRecording();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.i(TAG,  rightNow.getTimeInMillis() + " stopRecording");
                    isRecording = false;
                    intDelayMillis = 200;
                    //audioRecognizer.listenSound();
                    // send hashes to rest
                } else {
                    try {
                        recorder.startRecording();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.i(TAG,  rightNow.getTimeInMillis() + " startRecording");
                    isRecording = true;
                    intDelayMillis = 2000;
                }

                if(isWatching) {
                    //mWatchingTextView.setText("start");
                    MyHandler mHandler = new MyHandler();
                    mHandler.postDelayed(this, intDelayMillis);
                } else {
                    try {
                        recorder.stopRecording();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG,  rightNow.getTimeInMillis() + " stopRecording");
                }
            }
        }
    }


    private void watching() {


        //mWatchingTextView.setText("start");
        mHandler.postDelayed(mRunnable, intDelayMillis);
    }

    //private final Mp3Recorder audioRecognizer = new AudioRecognizerWindow();
    private MyRunnable mRunnable = new MyRunnable(this); //
    private Permission mPermission = new Permission(this);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Timber.w("ONCREATE");

        init();

    }



    private void init() {

        // setup recycler view adapter
        mAdapter = new CostItemAdapter(this, this);

        //recorder = new Mp3Recorder();

        // setup recycler view
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // instantiate the presenter
        mMainPresenter = new MainPresenterImpl(
                ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
                new CostRepositoryImpl(this)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mPermission.checkPermissionForRECORD_AUDIO() == true) {
                allowRECORD_AUDIO = true;
                Log.i(TAG,  "if1");
            } else {
                allowRECORD_AUDIO = false;
                Log.i(TAG,  "if2");
            }
        } else {
            allowRECORD_AUDIO = true;
            Log.i(TAG,  "if3");
        }
        allowRECORD_AUDIO = true; //TODO: tmp

        mButtonStartWatching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isWatching) {
                    isWatching = false;
                    //watchingThread.stop();
                } else {
                    isWatching = true;
                    if(allowRECORD_AUDIO){
                        watching();
                    }
                }


            }
        });


        // create a dummy account if it doesn't yet exist
        DummyAccountProvider.CreateSyncAccount(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMainPresenter.resume();

        // reset the layout
        mRevealLayout.setVisibility(View.INVISIBLE);

        Timber.w("ONRESUME");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add_cost:
                // intent to start another activity
                final Intent intent = new Intent(MainActivity.this, AddCostActivity.class);

                // do the animation
                AnimatorFactory.enterReveal(mRevealLayout, intent, MainActivity.this);

                break;
            case R.id.action_about:
                final Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check if everything is ok
        if (requestCode == EDIT_COST_REQUEST && resultCode == RESULT_OK) {

            // let the user know the edit succeded
            Toast.makeText(this, "Updated!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void showCosts(List<DailyTotalCost> costs) {
        // signal the adapter that it has data to show
        mAdapter.addNewCosts(costs);
    }

    @Override
    public void showWatcherButton() {
        // signal the adapter that it has data to show
        mAdapter.addWatcherButton();
    }

    @Override
    public void onClickDeleteCost(final long costId) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mMainPresenter.deleteCost(costId);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Delete this cost?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    @Override
    public void onCostDeleted(Cost cost) {
        // we deleted some data, RELOAD ALL THE THINGS!
        mMainPresenter.getAllCosts();
    }

    @Override
    public void onClickEditCost(long costId, int position) {

        // intent to start another activity
        final Intent intent = new Intent(MainActivity.this, EditCostActivity.class);
        intent.putExtra(EXTRA_COST_ID, costId);

        startActivityForResult(intent, EDIT_COST_REQUEST);
    }

    @Override
    public void onClickStartWatching() {

    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {

    }

    @Override
    public void showError(String message) {
    }


    // Request Call Back Method To check permission is granted by user or not for MarshMallow...
    /*@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case COMMON_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    // This method will be executed once the timer is over
                    // Start your app main activity

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }*/


}
