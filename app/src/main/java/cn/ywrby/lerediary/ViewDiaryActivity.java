package cn.ywrby.lerediary;

import android.content.Intent;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import cn.ywrby.lerediary.db.Diary;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.widemouth.library.wmview.WMTextEditor;

import static com.widemouth.library.wmview.WMTextEditor.TYPE_NON_EDITABLE;

public class ViewDiaryActivity extends AppCompatActivity {

    private ImageView view_weather;
    private TextView view_date;
    private WMTextEditor view_content;
    private ImageView view_cover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        Intent intent=getIntent();
        Diary diary=intent.getParcelableExtra("diary");

        view_weather=findViewById(R.id.view_weather);
        view_date=findViewById(R.id.view_date);
        view_content=findViewById(R.id.view_content);
        view_cover=findViewById(R.id.view_cover);


        view_weather.setImageResource(initWeather(diary.getWeather()));
        view_date.setText(diary.getDate()+"-"+diary.getTime());
        view_content.fromHtml(diary.getContent());
        view_content.setEditorType(TYPE_NON_EDITABLE);

        Toolbar toolbar=findViewById(R.id.view_toolbar);
        CollapsingToolbarLayout collapsingToolbarLayout=findViewById(R.id.view_collapsing_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        collapsingToolbarLayout.setTitle(diary.getTitle());

        String coverPath=diary.getCover();
        if(coverPath!=null) {
            Glide.with(this).load(coverPath).into(view_cover);
        }else {
            Glide.with(this).load(R.drawable.default_cover).into(view_cover);
        }
    }

    //获取天气信息
    private int initWeather(int weather){
        switch (weather) {
            case 1:
                return R.drawable.mood1;
            case 2:
                return R.drawable.mood2;
            case 3:
                return R.drawable.mood3;
            case 4:
                return R.drawable.mood4;
            case 5:
                return R.drawable.mood5;
            case 6:
                return R.drawable.mood6;
            case 7:
                return R.drawable.mood7;
            case 8:
                return R.drawable.mood8;
            default:
                break;
        }
        return 0;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
