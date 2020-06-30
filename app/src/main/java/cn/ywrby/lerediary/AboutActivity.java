package cn.ywrby.lerediary;

import android.view.MenuItem;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //设置ToolBar
        Toolbar toolbar=findViewById(R.id.about_toolbar);
        CollapsingToolbarLayout collapsingToolbarLayout=findViewById(R.id.about_collapsing_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();  //这里的ActionBar实际上已经是通过ToolBar实现
        if(actionBar!=null){
            //Toolbar最左侧的按钮被称为HomeAsUp按钮，默认图标是返回箭头，作用就是返回上一个活动
            actionBar.setDisplayHomeAsUpEnabled(true);  //允许显示返回按钮
        }
        ImageView cover=findViewById(R.id.about_cover);
        Glide.with(AboutActivity.this).load(R.drawable.default_cover4).into(cover);
        //标题
        collapsingToolbarLayout.setTitle("关于LereDiary");



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
