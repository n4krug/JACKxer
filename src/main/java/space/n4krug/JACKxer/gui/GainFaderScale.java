package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.jackManager.Gain;

public class GainFaderScale extends Region {

    private static final double LABEL_GAP = 6.0;
    private static final double MIN_SLIDER_WIDTH = 18.0;
    private static final double MAX_SLIDER_WIDTH = 28.0;
    private static final double MIN_SCALE_WIDTH = 56.0;
    private static final double MAX_SCALE_WIDTH = 84.0;
    private static final double FALLBACK_THUMB_SIZE = 16.0;

    private final Slider slider;
    private final List<DbMark> dbMarks = new ArrayList<>();
    private final double preferredScaleWidth;
    private final Rectangle clip = new Rectangle();
    private final Rectangle sliderClip = new Rectangle();

    public GainFaderScale(Gain gainClient, ControlParameter<Float> gainParam) {
        getStyleClass().add("gain-fader-scale");
        setClip(clip);

        slider = new Slider(0, 1, gainParam.getValue());
        slider.setOrientation(Orientation.VERTICAL);
        slider.setMinSize(0, 0);
        slider.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        slider.setClip(sliderClip);

        gainParam.addListener(slider::adjustValue);
        slider.valueProperty().addListener((obs, oldValue, newValue) ->
                gainParam.setNormalized(newValue.floatValue()));

        getChildren().add(slider);

        addDbMark(gainClient, "+6 dB", 6f);
        addDbMark(gainClient, "+3 dB", 3f);
        addDbMark(gainClient, "0 dB", 0f);
        addDbMark(gainClient, "-5 dB", -5f);
        addDbMark(gainClient, "-15 dB", -15f);
        addDbMark(gainClient, "-30 dB", -30f);
        addDbMark(gainClient, "-50 dB", -50f);
        addDbMark("-\u221e dB", 0f);

        preferredScaleWidth = dbMarks.stream()
                .mapToDouble(mark -> mark.label.prefWidth(-1))
                .max()
                .orElse(MIN_SCALE_WIDTH);
    }

    public Slider getSlider() {
        return slider;
    }

    public double computeScaleWidth(double availableWidth) {
        double desired = Math.max(preferredScaleWidth, availableWidth * 0.18);
        return clamp(desired, MIN_SCALE_WIDTH, MAX_SCALE_WIDTH);
    }

    public double computeSliderWidth(double availableWidth) {
        return clamp(availableWidth * 0.10, MIN_SLIDER_WIDTH, MAX_SLIDER_WIDTH);
    }

    public double computeBlockWidth(double availableWidth) {
        return computeSliderWidth(availableWidth) + LABEL_GAP + computeScaleWidth(availableWidth);
    }

    private void addDbMark(Gain gainClient, String text, float db) {
        addDbMark(text, gainClient.dbToNormalized(db));
    }

    private void addDbMark(String text, float normalized) {
        Label label = new Label(text);
        label.getStyleClass().add("gain-fader-db-label");
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMouseTransparent(true);
        dbMarks.add(new DbMark(label, normalized));
        getChildren().add(label);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            clip.setWidth(0);
            clip.setHeight(0);
            sliderClip.setWidth(0);
            sliderClip.setHeight(0);
            slider.resizeRelocate(0, 0, 0, 0);
            for (DbMark mark : dbMarks) {
                mark.label.resizeRelocate(0, 0, 0, 0);
            }
            return;
        }

        clip.setWidth(w);
        clip.setHeight(h);

        double sliderW = computeSliderWidth(w);
        double sliderX = 0;
        slider.resizeRelocate(sliderX, 0, sliderW, h);
        sliderClip.setX(0);
        sliderClip.setY(0);
        sliderClip.setWidth(sliderW);
        sliderClip.setHeight(h);

        double scaleX = sliderX + sliderW + LABEL_GAP;
        double scaleW = Math.max(0, w - scaleX);

        TrackBounds trackBounds = measureTrackBounds(h);
        for (DbMark mark : dbMarks) {
            double labelH = snapSizeY(mark.label.prefHeight(scaleW));
            double centerY = trackBounds.top + (1.0 - mark.normalized) * trackBounds.height;
            double labelY = clamp(centerY - labelH / 2.0, 0, Math.max(0, h - labelH));
            mark.label.resizeRelocate(scaleX, labelY, scaleW, labelH);
        }
    }

    private TrackBounds measureTrackBounds(double sliderH) {
        slider.applyCss();
        double thumbSize = FALLBACK_THUMB_SIZE;
        var thumb = slider.lookup(".thumb");
        if (thumb != null) {
            double actualThumbHeight = thumb.getBoundsInLocal().getHeight();
            if (actualThumbHeight > 0) {
                thumbSize = actualThumbHeight;
            }
        }

        double top = Math.min(thumbSize / 2.0, sliderH);
        double height = Math.max(0, sliderH - thumbSize);
        return new TrackBounds(top, height);
    }

    @Override
    protected double computePrefWidth(double height) {
        return MIN_SLIDER_WIDTH + LABEL_GAP + MAX_SCALE_WIDTH;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 140;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private record DbMark(Label label, float normalized) { }

    private record TrackBounds(double top, double height) { }
}
