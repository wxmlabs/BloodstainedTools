package com.wxmlabs.bloodstained;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BloodstainedDlcFixMain {
    public static void main(String[] args) throws IOException {
        String needsFixSav = "Story_Slot0.sav";
        String localValidSav = "Story_Slot1.sav";
        String fixedSav = "Story_Slot2.sav";
        File needsFixFile = new File(needsFixSav);
        File localValidFile = new File(localValidSav);

        BloodstainedSave localValid = new BloodstainedSave();
        localValid.load(new FileInputStream(localValidFile));
        BloodstainedDlc dlc = localValid.getDlc();

        BloodstainedSave needsFix = new BloodstainedSave();
        needsFix.load(new FileInputStream(needsFixFile));
        needsFix.setDlc(dlc);
        needsFix.store(new FileOutputStream(fixedSav));
    }
}
