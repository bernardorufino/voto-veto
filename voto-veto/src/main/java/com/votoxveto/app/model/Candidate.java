package com.votoxveto.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.WeakHashMap;

public class Candidate implements Parcelable, Comparable<Candidate> {

    private static final Map<String, Candidate> INSTANCE_POOL = new WeakHashMap<>();

    // Not thread-safe
    public static Candidate getOrCreateById(String id) {
        Candidate instance = INSTANCE_POOL.get(id);
        if (instance == null) {
            instance = new Candidate(id);
            INSTANCE_POOL.put(id, instance);
        }
        return instance;
    }

    private String mId;
    private String mName;
    private String mImageUrl;
    private String mState;

    private Candidate(String id) {
        mId = id;
    }

    private Candidate(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mImageUrl = in.readString();
        mState = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mImageUrl);
        dest.writeString(mState);
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    public static final Creator<Candidate> CREATOR = new Creator<Candidate>() {

        @Override
        public Candidate createFromParcel(Parcel in) {
            return new Candidate(in);
        }

        @Override
        public Candidate[] newArray(int size) {
            return new Candidate[size];
        }
    };

    @Override
    public int compareTo(Candidate another) {
        return getName().compareTo(another.getName());
    }
}
