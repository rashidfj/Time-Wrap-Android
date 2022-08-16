package com.peek.time.wrap.scan.timewrap.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LiveDataModel extends ViewModel {

    MutableLiveData<String> mutableLiveData=new MutableLiveData<>();

    public void setText(String s)
    {
        mutableLiveData.setValue(s);
    }
    public MutableLiveData<String> getText()
    {
        return mutableLiveData;
    }

}
