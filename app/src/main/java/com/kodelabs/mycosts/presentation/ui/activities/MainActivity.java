package com.kodelabs.mycosts.presentation.ui.activities;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.List;
import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.codetail.widget.RevealFrameLayout;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements MainPresenter.View {

    public static final String EXTRA_COST_ID = "extra_cost_id_key";

    public static final int EDIT_COST_REQUEST = 0;
    public static boolean isWatching = false;

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

    private static class MyHandler extends Handler {}
    private final MyHandler mHandler = new MyHandler();

    public static class MyRunnable implements Runnable {
        private final WeakReference<Activity> mActivity;

        public MyRunnable(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            Activity activity = mActivity.get();
            if (activity != null) {
                TextView mWatchingTextView = (TextView) activity.findViewById(R.id.watching_text_view);
                mWatchingTextView.setText("stop");

                if(isWatching) {
                    mWatchingTextView.setText("start");
                    MyHandler mHandler = new MyHandler();
                    mHandler.postDelayed(this, 2000);
                }
            }
        }
    }

    private void watching() {
        mWatchingTextView.setText("start");
        mHandler.postDelayed(mRunnable, 2000);

    }

    private MyRunnable mRunnable = new MyRunnable(this);



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

        //final Mp3Recorder recorder = new Mp3Recorder();
        //recorder.startRecording();
        //recorder.stopRecording();


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

        mButtonStartWatching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isWatching) {
                    isWatching = false;
                    //watchingThread.stop();
                } else {
                    isWatching = true;
                    watching();
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
}
