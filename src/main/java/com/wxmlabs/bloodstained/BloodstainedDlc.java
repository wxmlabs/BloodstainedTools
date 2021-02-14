package com.wxmlabs.bloodstained;

import static com.wxmlabs.bloodstained.BloodstainedSave.End_of_None;

public class BloodstainedDlc implements BloodstainedObject {
    static final byte[] Begin_of_HasDLCs = new byte[]{0x48, 0x61, 0x73, 0x44, 0x4c, 0x43, 0x73, 0x00}; // HasDLCs
    private byte[] content;

    BloodstainedDlc() {
    }

    @Override
    public byte[] getBeginTag() {
        return Begin_of_HasDLCs;
    }

    @Override
    public byte[] getEndTag() {
        return End_of_None;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public void setContent(byte[] content) {
        this.content = content;
    }
}
