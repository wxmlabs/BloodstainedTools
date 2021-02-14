package com.wxmlabs.bloodstained;

public interface BloodstainedObject {
    byte[] getBeginTag();

    byte[] getEndTag();

    byte[] getContent();

    void setContent(byte[] content);
}
