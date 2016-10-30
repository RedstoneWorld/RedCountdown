package de.redstoneworld.redcountdown;

import java.util.Map;

class RedCountdownTitle {
    private final int lowest;
    private String title = "";
    private String subTitle = "";
    private String sound = null;
    private float soundVolume = 100.0f;
    private float soundPitch = 1.0f;

    public RedCountdownTitle(int lowest, String title, String subTitle) {
        this.lowest = lowest;
        this.title = title;
        this.subTitle = subTitle;
    }

    public RedCountdownTitle(Map<?, ?> title) throws IllegalArgumentException {
        if (!(title.containsKey("lowest") && title.get("lowest") instanceof Integer)) {
            throw new IllegalArgumentException("lowest is not an Integer?");
        }

        this.lowest = (Integer) title.get("lowest");

        if (title.containsKey("title")) {
            this.title = (String) title.get("title");
        }

        if (title.containsKey("subtitle")) {
            this.subTitle = (String) title.get("subtitle");
        }

        if (title.containsKey("sound")) {
            Map<?, ?> soundSection = (Map<?, ?>) title.get("sound");
            if (!soundSection.containsKey("id")) {
                throw new IllegalArgumentException("sound has no id?");
            }
            sound = (String) soundSection.get("id");

            if (soundSection.containsKey("volume")) {
                Object volume = soundSection.get("volume");

                if (volume instanceof Number) {
                    soundVolume = ((Number) volume).floatValue();
                } else if (volume instanceof String) {
                    soundVolume = Float.parseFloat((String) volume);
                }
            }

            if (soundSection.containsKey("pitch")) {
                Object pitch = soundSection.get("pitch");
                if (pitch instanceof Number) {
                    soundPitch = ((Number) pitch).floatValue();
                } else if (pitch instanceof String) {
                    soundPitch = Float.parseFloat((String) pitch);
                }
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{lowest=" + lowest + ",title=" + title + ",subTitle=" + subTitle + ",sound=" + sound + ",soundVolume=" + soundVolume + ",soundPitch=" + soundPitch + "}";
    }

    public int getLowest() {
        return lowest;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getSound() {
        return sound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }
}
