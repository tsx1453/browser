package de.baumann.browser.utils;

import java.util.ArrayList;
import java.util.List;

import de.baumann.browser.Browser;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.storage.SP;

public class DefaultHomeEntranceHelper {

    public static void insertDefaultHomeEntrance() {
        if (!SP.getInstance().isFirstLaunch()) {
            return;
        }
        List<Record> recordList = new ArrayList<>();
        recordList.add(new Record("YouTube", "https://m.youtube.com/", 0, 0, SP.getInstance().getAndIncreaseCounter(), false, false, 0));
        recordList.add(new Record("Twitter", "https://twitter.com/", 0, 0, SP.getInstance().getAndIncreaseCounter(), false, false, 0));
        recordList.add(new Record("Facebook", "https://www.facebook.com/", 0, 0, SP.getInstance().getAndIncreaseCounter(), false, false, 0));
        recordList.add(new Record("Instagram", "https://www.instagram.com/", 0, 0, SP.getInstance().getAndIncreaseCounter(), false, false, 0));

        RecordAction recordAction = new RecordAction(Browser.getConfig().context);
        recordAction.open(true);
        for (Record record : recordList) {
            recordAction.addStartSite(record);
        }
        recordAction.close();
    }

}
