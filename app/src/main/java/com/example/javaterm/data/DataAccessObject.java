package com.example.javaterm.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataAccessObject {

    @Query("SELECT * FROM ToDo ORDER BY taskDateTime")
    List<ToDo> getAllTasksAscending();

    @Query("SELECT * FROM ToDo WHERE isCompleted = 1 ORDER BY taskDateTime")
    List<ToDo> getCompletedTasksAscending();

    @Query("SELECT * FROM ToDo WHERE isCompleted = 0 ORDER BY taskDateTime")
    List<ToDo> getUncompletedTasksAscending();

    @Query("SELECT * FROM ToDo ORDER BY taskDateTime DESC")
    List<ToDo> getAllTasksDescending();

    @Query("SELECT * FROM ToDo WHERE isCompleted = 1 ORDER BY taskDateTime DESC")
    List<ToDo> getCompletedTasksDescending();

    @Query("SELECT * FROM ToDo WHERE isCompleted = 0 ORDER BY taskDateTime DESC")
    List<ToDo> getUncompletedTasksDescending();

    @Query("SELECT * FROM ToDo WHERE id = :id")
    ToDo getById(Long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ToDo toDo);

    @Delete
    void delete(ToDo toDo);
}
