package com.example.javaterm;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.javaterm.data.Notification;
import com.example.javaterm.data.ToDo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TaskActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener, CompoundButton.OnCheckedChangeListener {

    private enum NotificationTimeBefore {
        NO("Мэдэгдэл хүлээн авахгүй", 0),
        HOUR("1 цагийн өмнө", 60),
        DAY("Нэг өдрийн өмнө", 1440),
        WEEK("Долоо хоногийн өмнө", 10080);

        public static int getPosByMinutes(int minutes) {
            NotificationTimeBefore[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getMinutes() == minutes)
                    return i;
            }
            return -1;
        }

        private final String friendlyName;
        private final int minutes;

        NotificationTimeBefore(String friendlyName, int minutes) {
            this.friendlyName = friendlyName;
            this.minutes = minutes;
        }

        public int getMinutes() {
            return minutes;
        }

        @Override
        public String toString() {
            return friendlyName;
        }
    }

    private EditText editTextTaskText;
    private CheckBox checkBoxTaskCompleted;
    private EditText editTextTaskDate;
    private EditText editTextTaskTime;
    private ImageButton buttonClearDate;
    private ImageButton buttonClearTime;
    private Spinner spinnerNotificationTimesBefore;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy",
            Locale.ENGLISH);

    private boolean isEditActivity;
    private ToDo toDo;
    private ToDo toDoBeforeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isEditActivity = getIntent().hasExtra("taskId");
        if (!isEditActivity) {
            setContentView(R.layout.activity_task2);
        } else {
            setContentView(R.layout.activity_task);
            checkBoxTaskCompleted = findViewById(R.id.checkBoxTaskCompleted);
            checkBoxTaskCompleted.setOnCheckedChangeListener(this);
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        editTextTaskText = findViewById(R.id.editTextTaskText);
        editTextTaskDate = findViewById(R.id.editTextTaskDate);
        editTextTaskTime = findViewById(R.id.editTextTaskTime);
        buttonClearDate = findViewById(R.id.buttonClearDate);
        buttonClearDate.setEnabled(false);
        buttonClearTime = findViewById(R.id.buttonClearTime);
        buttonClearTime.setEnabled(false);
        spinnerNotificationTimesBefore = findViewById(R.id.spinnerNotificationTimesBefore);
        spinnerNotificationTimesBefore.setEnabled(false);

        initializeSpinnerArrayAdapter();
        spinnerNotificationTimesBefore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toDo.setNotificationMinutesBefore(((NotificationTimeBefore) parent.getItemAtPosition(position)).getMinutes());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        initializeActivityViewElements();

        checkNotificationSetCorrect();
    }

    private void initializeActivityViewElements() {
        if (isEditActivity) {
            long taskId = getIntent().getExtras().getLong("taskId");
            toDo = App.getInstance().getTaskDatabase().taskDao().getById(taskId);
            toDoBeforeEdit = App.getInstance().getTaskDatabase().taskDao().getById(taskId);
        } else {
            toDo = new ToDo();
            toDoBeforeEdit = new ToDo();
            toDoBeforeEdit.setTaskText("");
        }

        editTextTaskText.setText(toDo.getTaskText());
        if (isEditActivity) {
            checkBoxTaskCompleted.setChecked(toDo.isCompleted());
        }
        if (toDo.getTaskDateTime() == null) {
            editTextTaskTime.setEnabled(false);
        } else {
            editTextTaskDate.setText(dateFormat.format(toDo.getTaskDateTime().getTime()));
            buttonClearDate.setEnabled(true);
            if (toDo.isTimeSet()) {
                editTextTaskTime.setText(timeFormat.format(toDo.getTaskDateTime().getTime()));
                buttonClearTime.setEnabled(true);
                spinnerNotificationTimesBefore.setEnabled(true);
                spinnerNotificationTimesBefore.setSelection(NotificationTimeBefore.getPosByMinutes(toDo.getNotificationMinutesBefore()));
            }
        }
    }

    private void initializeSpinnerArrayAdapter() {
        final ArrayAdapter<NotificationTimeBefore> adapter =
                new ArrayAdapter<NotificationTimeBefore>(this,
                        android.R.layout.simple_spinner_item, NotificationTimeBefore.values()) {
                    @Override
                    public boolean isEnabled(int position) {
                        if (position == 0)
                            return true;
                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.SECOND, 0);
                        long nowTime = now.getTime().getTime();
                        NotificationTimeBefore[] values = NotificationTimeBefore.values();
                        Calendar taskDateTime = toDo.getTaskDateTime();
                        long targetTime =
                                taskDateTime.getTime().getTime() - 60000L * values[position].getMinutes();
                        return nowTime < targetTime;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView,
                                                @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        if (position == 0) {
                            tv.setTextColor(Color.BLACK);
                            return view;
                        }
                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.SECOND, 0);
                        long nowTime = now.getTime().getTime();
                        NotificationTimeBefore[] values = NotificationTimeBefore.values();
                        Calendar taskDateTime = toDo.getTaskDateTime();
                        long targetTime =
                                taskDateTime.getTime().getTime() - 60000L * values[position].getMinutes();

                        if (nowTime < targetTime) {
                            tv.setTextColor(Color.BLACK);
                        } else {
                            tv.setTextColor(Color.GRAY);
                        }
                        return view;
                    }
                };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationTimesBefore.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditActivity) {
            getMenuInflater().inflate(R.menu.activity_task_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Are you sure?")
                    .setMessage("Delete task?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().getTaskDatabase().taskDao().delete(toDoBeforeEdit);
                            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(String.valueOf(toDoBeforeEdit.getId()));
                            finish();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        toDo.setCompleted(isChecked);
        buttonView.setText(toDo.isCompleted() ? "Task is completed!" :
                "Is task completed?");
    }

    public void setDate(View view) {
        Calendar dateTime = toDo.getTaskDateTime() != null ? toDo.getTaskDateTime() :
                Calendar.getInstance();
        new DatePickerDialog(this, this, dateTime.get(Calendar.YEAR),
                dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void setTime(View view) {
        Calendar dateTime = toDo.isTimeSet() ? toDo.getTaskDateTime() : Calendar.getInstance();
        new TimePickerDialog(this, this, dateTime.get(Calendar.HOUR_OF_DAY),
                dateTime.get(Calendar.MINUTE), true).show();
    }

    public void clearDate(View view) {
        toDo.setTaskDateTime(null);
        editTextTaskDate.setText(null);
        editTextTaskTime.setEnabled(false);
        buttonClearDate.setEnabled(false);
        clearTime(view);
    }

    public void clearTime(View view) {
        toDo.setTimeSet(false);
        editTextTaskTime.setText(null);
        buttonClearTime.setEnabled(false);
        spinnerNotificationTimesBefore.setSelection(0);
        spinnerNotificationTimesBefore.setEnabled(false);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (toDo.getTaskDateTime() == null) {
            Calendar dateTime = Calendar.getInstance();
            dateTime.set(year, month, dayOfMonth, 0, 0, 0);
            toDo.setTaskDateTime(dateTime);
            editTextTaskTime.setEnabled(true);
        } else {
            toDo.getTaskDateTime().set(year, month, dayOfMonth);
        }

        editTextTaskDate.setText(dateFormat.format(toDo.getTaskDateTime().getTime()));
        buttonClearDate.setEnabled(true);
        checkNotificationSetCorrect();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        toDo.getTaskDateTime().set(Calendar.HOUR_OF_DAY, hourOfDay);
        toDo.getTaskDateTime().set(Calendar.MINUTE, minute);
        toDo.setTimeSet(true);

        editTextTaskTime.setText(timeFormat.format(toDo.getTaskDateTime().getTime()));
        buttonClearTime.setEnabled(true);
        spinnerNotificationTimesBefore.setEnabled(true);
        checkNotificationSetCorrect();
    }

    private void checkNotificationSetCorrect() {
        if (toDo.getNotificationMinutesBefore() > 0) {
            Calendar now = Calendar.getInstance();
            long nowTime = now.getTime().getTime();
            long targetTime = toDo.getTaskDateTime().getTime().getTime();
            if (nowTime > targetTime) {
                spinnerNotificationTimesBefore.setSelection(0);
                toDo.setNotificationMinutesBefore(0);
            }
        }
    }

    public void acceptTask(View view) {
        toDo.setTaskText(editTextTaskText.getText().toString());
        if (toDo.getTaskText().replaceAll(" ", "").isEmpty()) {
            Toast toast = Toast.makeText(this, "Даалгаврын текст хоосон байх ёсгүй", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toDo.setId(App.getInstance().getTaskDatabase().taskDao().insert(toDo));
            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(String.valueOf(toDo.getId()));
            if (toDo.getNotificationMinutesBefore() > 0 && !toDo.isCompleted()) {
                long targetTime =
                        toDo.getTaskDateTime().getTime().getTime() - 60000L * toDo.getNotificationMinutesBefore();
                long nowTime = Calendar.getInstance().getTime().getTime();
                if (nowTime < targetTime) {
                    Data inputData = new Data.Builder()
                            .putString("title", "Don't forget, You have a task!")
                            .putString("text", toDo.getTaskText())
                            .build();
                    long delay = targetTime - nowTime;
                    OneTimeWorkRequest oneTimeWorkRequest =
                            new OneTimeWorkRequest.Builder(Notification.class)
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .addTag(String.valueOf(toDo.getId()))
                                    .build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
                }
            }
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        toDo.setTaskText(editTextTaskText.getText().toString());
        if (Objects.equals(toDoBeforeEdit, toDo)) {
            finish();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Хадгалалгүйгээр гарах уу?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .create();
            alertDialog.show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
