package com.example.myapplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
public class DummyDataProvider {
    public static List<BlinkData> generateDummyBlinkData(int durationInSeconds, int totalBlinks) {
        List<BlinkData> dummyDataList = new ArrayList<>();
        Random random = new Random();
        Set<Integer> blinkTimes = new HashSet<>();

        // Ensure totalBlinks do not exceed durationInSeconds
        totalBlinks = Math.min(totalBlinks, durationInSeconds);

        // Generate unique random times for blinks
        while (blinkTimes.size() < totalBlinks) {
            blinkTimes.add(random.nextInt(durationInSeconds));
        }

        // Initialize all seconds to 0 blinks
        for (int i = 0; i < durationInSeconds; i++) {
            dummyDataList.add(new BlinkData(i, 0));
        }

        // Set blinks to 1 at the random times
        for (int blinkTime : blinkTimes) {
            dummyDataList.get(blinkTime).setBlinkCount(1);
        }

        return dummyDataList;
    }
}

class BlinkData {
    private int second;
    private int blinkCount;

    public BlinkData(int second, int blinkCount) {
        this.second = second;
        this.blinkCount = blinkCount;
    }

    public int getSecond() {
        return second;
    }

    public int getBlinkCount() {
        return blinkCount;
    }
    public void setBlinkCount(int blinkCount) {
        this.blinkCount = blinkCount;
    }
}
