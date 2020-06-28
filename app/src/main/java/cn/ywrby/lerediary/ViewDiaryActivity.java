package cn.ywrby.lerediary;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.widemouth.library.wmview.WMTextEditor;

import static com.widemouth.library.wmview.WMTextEditor.TYPE_NON_EDITABLE;

public class ViewDiaryActivity extends AppCompatActivity {

    private ImageView view_weather;
    private TextView view_date;
    private WMTextEditor view_content;
    private ImageView view_cover;
    private FloatingActionButton view_edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);
        //获取传入的日记对象
        Intent intent=getIntent();
        final Diary diary=intent.getParcelableExtra("diary");
        //初始化控件对象
        view_weather=findViewById(R.id.view_weather);
        view_date=findViewById(R.id.view_date);
        view_content=findViewById(R.id.view_content);
        view_cover=findViewById(R.id.view_cover);
        view_edit=findViewById(R.id.view_edit);

        //载入信息
        view_weather.setImageResource(initWeather(diary.getWeather()));
        view_date.setText(diary.getDate()+"-"+diary.getTime());
        view_content.fromHtml(diary.getContent());
        view_content.setEditorType(TYPE_NON_EDITABLE);
        //响应编辑信息
        view_edit.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                Intent edit_intent=new Intent(ViewDiaryActivity.this,EditDiaryActivity.class);
                edit_intent.putExtra("diary",diary);
                startActivity(edit_intent);
                finish();
            }
        });

        Toolbar toolbar=findViewById(R.id.view_toolbar);
        CollapsingToolbarLayout collapsingToolbarLayout=findViewById(R.id.view_collapsing_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        //返回键
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //标题
        collapsingToolbarLayout.setTitle(diary.getTitle());
        //显示封面
        String coverPath=diary.getCover();
        if(coverPath!=null) {
            Glide.with(this).load(coverPath).into(view_cover);
        }else {
            //使用默认的图片作为封面
            Glide.with(this).load(R.drawable.default_cover).into(view_cover);
        }


    }

    //

    /**
     * 获取天气信息
     * @param weather 当前日记的索引值
     * @return 天气图片的ID
     */
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
                Intent intent=new Intent(ViewDiaryActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(ViewDiaryActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
