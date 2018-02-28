package com.qb.xrealsys.ifafu;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.OptionsPickerView;
import com.qb.xrealsys.ifafu.delegate.TitleBarButtonOnClickedDelegate;
import com.qb.xrealsys.ifafu.dialog.CourseInfoDialog;
import com.qb.xrealsys.ifafu.model.Course;
import com.qb.xrealsys.ifafu.tool.ConfigHelper;
import com.qb.xrealsys.ifafu.tool.GlobalLib;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SyllabusActivity extends AppCompatActivity implements
        View.OnClickListener,
        TitleBarButtonOnClickedDelegate,
        OptionsPickerView.OnOptionsSelectListener {

    private static final int horizontalTabNum = 7;

    private static final int verticalTabNum   = 12;

    private static final String[] studyBeginTime = new String[] {
            "8:00", "8:50", "9:55", "10:45", "11:35",
            "14:00", "14:50", "15:35", "16:40",
            "18:25", "19:15", "20:05"};

    private static final String[] weekDayName = new String[] {
            "周日", "周一", "周二", "周三", "周四", "周五", "周六",};

    private static final String[] baseColors = new String[] {
            "#fa474b", "#0273fe", "#fe7f02", "#b956f8", "#38d3a9",
            "#48d9f8", "#f0c83c", "#a9d53c", "#fcb304", "#f784e3",
            "#b8773a", "#2f2f2f", "#8e7fa7", "#6493b5", "#66a752"};

    private MainApplication         mainApplication;

    private SyllabusController      syllabusController;

    private TitleBarController      titleBarController;

    private ConfigHelper            configHelper;

    private LoadingViewController   loadingViewController;

    private RelativeLayout          syllbusContent;

    private LinearLayout            noDataView;

    private boolean                 isDraw;

    private int                     selectedWeek;

    private int                     nowWeek;

    private List<TextView>          coursesView;

    private Map<String, String>     mapNameToColor;

    private Map<Integer, Course>    mapIdToCourse;

    private OptionsPickerView       optionsPickerView;

    private CourseInfoDialog        courseInfoDialog;

    private List<String>            options;

    private TextView                pageTitle;

    private int                     contentWidth;

    private int                     contentHeight;

    private int                     titleWidth;

    private int                     titleHeight;

    private int                     tabWidth;

    private int                     tabHeight;

    private boolean                 isInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syllabus);

        isDraw          = true;
        isInit          = true;
        noDataView      = findViewById(R.id.noDataView);
        pageTitle       = findViewById(R.id.pagetitle);
        syllbusContent  = findViewById(R.id.syllabusContent);
        syllbusContent.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        if (isDraw) {
                            drawSyllabus();
                            loadingViewController.cancel();
                            isDraw = false;
                        }
                    }
                });

        mainApplication         = (MainApplication) getApplication();
        syllabusController      = mainApplication.getSyllabusController();
        loadingViewController   = new LoadingViewController(this);
        titleBarController      = new TitleBarController(this);
        coursesView             = new ArrayList<>();
        mapNameToColor          = new HashMap<>();
        mapIdToCourse           = new HashMap<>();
        courseInfoDialog        = new CourseInfoDialog(this);

        configHelper    = mainApplication.getConfigHelper();
        selectedWeek    = GlobalLib.GetNowWeek(configHelper.GetValue("nowTermFirstWeek"));
        nowWeek         = selectedWeek;

        String title;
        if (nowWeek < 1 || nowWeek > 24) {
            title = "放假中";
        } else {
            title = String.format(
                    Locale.getDefault(), "第%d周", nowWeek);
        }
        titleBarController
                .setHeadBack()
                .setTwoLineTitle(
                        title,
                        syllabusController.GetNowStudyTime(getString(R.string.format_study_time)))
                .setOnClickedListener(this);

        initOptionsPickerView();
        loadingViewController.show();
    }

    private void initOptionsPickerView() {
        pageTitle.setOnClickListener(this);
        optionsPickerView = new OptionsPickerView.Builder(
                SyllabusActivity.this,
                this)
                .setLinkage(false)
                .setSubmitText("确定")
                .setTitleSize(13)
                .setTitleText("选择目标周")
                .setTitleColor(Color.parseColor("#157efb"))
                .build();

        options = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            options.add(String.format(Locale.getDefault(), "第%d周", i));
        }
        optionsPickerView.setPicker(options);
        if (nowWeek > 0) {
            optionsPickerView.setSelectOptions(nowWeek - 1);
        }
    }

    private String[] makeDateOnWeek() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "MM-dd", Locale.getDefault());
        String[] answer     = new String[7];
        long plus = (selectedWeek - nowWeek) *  7L * 24L * 3600000L;
        long time = System.currentTimeMillis() + plus;

        calendar.setTime(new Date(time));
        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i + 1);
            Date date = calendar.getTime();
            answer[i] = simpleDateFormat.format(date.getTime());
        }

        return answer;
    }

    private void drawSyllabus() {
        if (isInit) {
            contentWidth    = syllbusContent.getWidth();
            contentHeight   = syllbusContent.getHeight();
            titleWidth      = contentWidth / (horizontalTabNum * 2 + 1);
            titleHeight     = titleWidth / 4 * 5;
            tabWidth        = titleWidth * 2;
            tabHeight       = (contentHeight - titleHeight) / verticalTabNum;

            drawTitle(titleWidth, titleHeight, tabWidth, tabHeight);
            isInit = false;
        }

        drawContent(titleWidth, titleHeight, tabWidth, tabHeight);
    }

    private void drawContent(int titleWidth, int titleHeight, int tabWidth, int tabHeight) {
        if (selectedWeek < 1 || selectedWeek > 24) {
            noDataView.setVisibility(View.VISIBLE);
            return;
        }
        noDataView.setVisibility(View.INVISIBLE);

        for (TextView view: coursesView) {
            syllbusContent.removeView(view);
        }
        coursesView.clear();

        List<List<Course>> data = syllabusController.GetCourseInfoByWeek(selectedWeek);
        int[] baseColorIndex       = new int[baseColors.length];
        int   baseColorSwitchLimit = baseColors.length;
        for (int i = 0; i < baseColors.length; i++) {
            baseColorIndex[i] = i;
        }
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < 7; i++) {
            List<Course> courseList = data.get(i);
            for (Course course: courseList) {
                int courseBegin  = course.getBegin();
                int courseLength = course.getEnd() - course.getBegin() + 1;

                //  Switch a Color
                String color;
                if (mapNameToColor.containsKey(course.getName())) {
                    color = mapNameToColor.get(course.getName());
                } else {
                    int index = random.nextInt(baseColorSwitchLimit);
                    color = baseColors[baseColorIndex[index]];
                    baseColorIndex[index] = baseColorIndex[baseColorSwitchLimit - 1];
                    baseColorSwitchLimit--;
                    mapNameToColor.put(course.getName(), color);
                }

                drawCourseView(
                        tabWidth, tabHeight * courseLength,
                        titleWidth + tabWidth * i,
                        titleHeight + tabHeight * (courseBegin - 1),
                        String.format(Locale.getDefault(), "%s\n@%s",
                                course.getName(), course.getAddress()),
                        color, course);
            }
        }

        if (coursesView.isEmpty()) {
            noDataView.setVisibility(View.VISIBLE);
        }
    }

    private void drawTitle(int titleWidth, int titleHeight, int tabWidth, int tabHeight) {
        //  draw setting button
        drawSettingBtn(titleWidth,titleHeight);

        //  draw horizontal title
        String[] date = makeDateOnWeek();
        for (int i = 0; i < horizontalTabNum; i++) {
            drawTitleItem(
                    tabWidth, titleHeight,
                    titleWidth + i * tabWidth, 0,
                    drawTitleTextView(weekDayName[i], true, 12, "#000000"),
                    drawTitleTextView(date[i], false, 10, "#aaaaaa"));
        }

        //  draw vertical title
        for (int i = 0; i < verticalTabNum; i++) {
            drawTitleItem(
                    titleWidth, tabHeight - 1,
                    0, titleHeight + i * tabHeight,
                    drawTitleTextView(studyBeginTime[i], false, 10, "#aaaaaa"),
                    drawTitleTextView(String.valueOf(i + 1), false, 16, "#aaaaaa"));
        }
    }

    private void drawCourseView(
            int width, int height,
            int x, int y,
            String content, String color,
            Course course) {
        TextView courseView = new TextView(this);
        RelativeLayout.LayoutParams courseViewParams
                = new RelativeLayout.LayoutParams(width, height);
        courseViewParams.topMargin = y;
        courseViewParams.setMarginStart(x);
        courseView.setLayoutParams(courseViewParams);
        courseView.setGravity(Gravity.CENTER);
        courseView.setTextColor(Color.parseColor("#ffffff"));
        courseView.setTextSize(10);
        courseView.setText(content);
        courseView.setBackgroundColor(Color.parseColor(color));
        int newId = View.generateViewId();
        courseView.setId(newId);
        courseView.setOnClickListener(this);
        mapIdToCourse.put(newId, course);

        syllbusContent.addView(courseView);
        coursesView.add(courseView);
    }

    private void drawTitleItem(
            int width, int height,
            int x, int y,
            TextView oneView, TextView twoView) {
        LinearLayout titleItem = new LinearLayout(this);
        RelativeLayout.LayoutParams titleItemParams
                = new RelativeLayout.LayoutParams(width, height);
        titleItemParams.setMarginStart(x);
        titleItemParams.topMargin = y;
        titleItem.setLayoutParams(titleItemParams);
        titleItem.setBackgroundColor(Color.parseColor("#ffffff"));
        titleItem.setOrientation(LinearLayout.VERTICAL);
        titleItem.setGravity(Gravity.CENTER_HORIZONTAL);
        titleItem.addView(oneView);
        titleItem.addView(twoView);

        syllbusContent.addView(titleItem);
    }

    private void drawSettingBtn(int width, int height) {
        LinearLayout settingBtnContent = new LinearLayout(this);
        settingBtnContent.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
        settingBtnContent.setGravity(Gravity.CENTER);
        settingBtnContent.setBackgroundColor(Color.parseColor("#ffffff"));

        ImageView settingBtn = new ImageView(this);
        settingBtn.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
        settingBtn.setImageResource(R.drawable.icon_setting);
        settingBtn.setScaleX(0.8f);
        settingBtn.setScaleY(0.8f);

        settingBtnContent.addView(settingBtn);
        syllbusContent.addView(settingBtnContent);
    }

    private TextView drawTitleTextView(String text, boolean isBold, int size, String color) {
        TextView oneView = new TextView(this);
        oneView.setTextColor(Color.parseColor(color));
        oneView.getPaint().setFakeBoldText(isBold);
        oneView.setTextSize(size);
        oneView.setText(text);
        oneView.setGravity(Gravity.CENTER);

        return oneView;
    }

    @Override
    public void titleBarOnClicked(int id) {
        switch (id) {
            case R.id.headback:
                finish();
                break;
        }
    }

    @Override
    public void onOptionsSelect(int options1, int options2, int options3, View v) {
        selectedWeek = options1 + 1;
        isDraw = true;
        pageTitle.setText(options.get(options1));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.pagetitle:
                optionsPickerView.show();
                break;
            default:
                Course course = mapIdToCourse.get(id);
                courseInfoDialog.show(course);
                break;
        }
    }
}