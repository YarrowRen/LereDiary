package cn.ywrby.lerediary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

public class ViewDiaryAdapter extends PagerAdapter {

    private Context mContext;
    private List<View> mViewList;
    private int startPosition;



    public ViewDiaryAdapter(List<View> mViewList) {
        this.mViewList = mViewList;
    }
    public ViewDiaryAdapter(List<View> mViewList,int startPosition) {
        this.mViewList = mViewList;
        this.startPosition=startPosition;
    }

    @Override
    public int getCount() {
        return mViewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mContext == null) {
            mContext = container.getContext();
        }
        View view=View.inflate(mContext,R.layout.activity_view_diary,null);


        container.addView(mViewList.get(position));
        return mViewList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewList.get(position));
    }
}
