package com.example.javaterm;

import android.app.Application;

import androidx.room.Room;

import com.example.javaterm.data.ToDoDatabase;

public class App extends Application {

    public static App instance;

    private ToDoDatabase toDoDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        toDoDatabase = Room.databaseBuilder(this, ToDoDatabase.class, "taskDatabase").allowMainThreadQueries().build();
    }

    public static App getInstance() {
        return instance;
    }

    public ToDoDatabase getTaskDatabase() {
        return toDoDatabase;
    }
}
