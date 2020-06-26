package cn.ywrby.lerediary.db;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import org.litepal.crud.LitePalSupport;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

//Parcelable是Android平台提供的序列化的接口

public class Diary extends LitePalSupport implements Parcelable {

    private String uuid;  //日记ID，用于唯一标识日记
    private String date;  //日期
    private String time;  //时间
    private int weather;  //天气ID
    private String title;  //标题
    private String content;  //内容
    private String cover;  //封面

    public Diary() {
        uuid=UUID.randomUUID().toString();
    }




    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getWeather() {
        return weather;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCover() {
        return cover;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }


    protected Diary(Parcel in) {
        uuid = in.readString();
        date = in.readString();
        time = in.readString();
        weather = in.readInt();
        title = in.readString();
        content = in.readString();
        cover = in.readString();
    }

    public static final Creator<Diary> CREATOR = new Creator<Diary>() {
        @Override
        public Diary createFromParcel(Parcel in) {
            return new Diary(in);
        }

        @Override
        public Diary[] newArray(int size) {
            return new Diary[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeInt(weather);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(cover);
    }

}
