package com.andrea.appsinstaller;

/**
 * Created by andrea.carlevato on 24/2/18.
 */

public class AppBuild {
    private String mName;
    private String mSubTitle;
    private String mURL;
    private String fileName;



    public AppBuild(String mName, String mSubTitle, String mURL, String fileName) {
        this.mName = mName;
        this.mSubTitle = mSubTitle;
        this.mURL = mURL;
        this.fileName = fileName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getURL() {
        return mURL;
    }

    public void setURL(String mURL) {
        this.mURL = mURL;
    }

    public String getSubTitle() {
        return mSubTitle;
    }

    public void setSubTitle(String mSubTitle) {
        this.mSubTitle = mSubTitle;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
