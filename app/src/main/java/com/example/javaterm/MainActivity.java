package com.example.javaterm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.javaterm.data.DataAccessObject;
import com.example.javaterm.data.ToDoAdapter;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private int currentFilterId;
    private int currentSortId;
    private boolean hideOverdue;

    private RecyclerView tasksRecyclerView;
    private ToDoAdapter toDoAdapter;

    public int getCurrentFilterId() {
        return currentFilterId;
    }

    public boolean isHideOverdue() {
        return hideOverdue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        toDoAdapter = new ToDoAdapter(this);
        tasksRecyclerView.setAdapter(toDoAdapter);
        float itemVerticalMargin = getResources().getDimension(R.dimen.item_vertical_margin);
        float bottomOffset = getResources().getDimension(R.dimen.last_item_bottom_offset);
        tasksRecyclerView.addItemDecoration(new ToDoAdapter.OffsetDecoration(itemVerticalMargin, bottomOffset));

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        hideOverdue = settings.getBoolean("hideOverdue", false);
        currentFilterId = settings.getInt("filterId", R.id.action_show_all);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refresh();
    }

    private void refresh() {
        DataAccessObject dataAccessObject = App.getInstance().getTaskDatabase().taskDao();
        if (currentFilterId == R.id.action_show_all) {
            Objects.requireNonNull(getSupportActionBar()).setTitle("All tasks");
            toDoAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    dataAccessObject.getAllTasksAscending() : dataAccessObject.getAllTasksDescending());
        } else if (currentFilterId == R.id.action_show_completed) {
            Objects.requireNonNull(getSupportActionBar()).setTitle("Completed");
            toDoAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    dataAccessObject.getCompletedTasksAscending() : dataAccessObject.getCompletedTasksDescending());
        } else if (currentFilterId == R.id.action_show_uncompleted) {
            Objects.requireNonNull(getSupportActionBar()).setTitle("Uncompleted");
            toDoAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    dataAccessObject.getUncompletedTasksAscending() :
                    dataAccessObject.getUncompletedTasksDescending());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_show_all || id == R.id.action_show_completed ||
                id == R.id.action_show_uncompleted || id == R.id.action_sort_ascending ||
                id == R.id.action_sort_descending) {
            currentFilterId = id;
            refresh();
        }
        return super.onOptionsItemSelected(item);
    }

    public void addTask(View view) {
        Intent intent = new Intent(this, TaskActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor settingsEditor =
                getSharedPreferences("settings", MODE_PRIVATE).edit();
        settingsEditor.putBoolean("hideOverdue", hideOverdue);
        settingsEditor.putInt("filterId", currentFilterId);
        settingsEditor.putInt("sortId", currentSortId);
        settingsEditor.apply();
    }
}