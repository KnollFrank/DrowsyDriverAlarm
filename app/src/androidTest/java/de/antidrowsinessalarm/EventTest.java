package de.antidrowsinessalarm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.antidrowsinessalarm.event.AwakeEvent;
import de.antidrowsinessalarm.event.DrowsyEvent;
import de.antidrowsinessalarm.event.EyesClosedEvent;
import de.antidrowsinessalarm.event.EyesOpenedEvent;
import de.antidrowsinessalarm.event.LikelyDrowsyEvent;
import de.antidrowsinessalarm.event.NormalEyeBlinkEvent;
import de.antidrowsinessalarm.event.SlowEyelidClosureEvent;
import de.antidrowsinessalarm.eventproducer.DefaultConfigFactory;
import de.antidrowsinessalarm.eventproducer.DrowsyEventDetector;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4.class)
public class EventTest {

    private Context appContext;
    private EventListener eventListener;
    private DrowsyEventDetector drowsyEventDetector;
    private FaceDetector detector;

    @Before
    public void setup() {
        this.setup(new SystemClock());
    }

    private void setup(Clock clock) {
        this.appContext = InstrumentationRegistry.getTargetContext();
        this.drowsyEventDetector =
                new DrowsyEventDetector(
                        DrowsyEventDetector.Config
                                .builder()
                                .withEyeOpenProbabilityThreshold(DefaultConfigFactory.getEyeOpenProbabilityThreshold())
                                .withConfig(DefaultConfigFactory.getConfig())
                                .withSlowEyelidClosureMinDuration(DefaultConfigFactory.getSlowEyelidClosureMinDuration())
                                .withTimeWindow(DefaultConfigFactory.getTimeWindow())
                                .build(),
                        true,
                        clock);
        this.eventListener = new EventListener();
        this.drowsyEventDetector.getEventBus().register(this.eventListener);
        this.detector =
                FaceTrackerActivity.createFaceDetector(
                        this.appContext,
                        new MultiProcessor.Builder<>(this.createFactory()).build());
    }

    @Test
    public void testOpen() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_opened, 0);

        // Thent
        assertThat(this.eventListener.filterEventsBy(EyesOpenedEvent.class), contains(instanceOf(EyesOpenedEvent.class)));
    }

    @Test
    public void testClose() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        // Then
        assertThat(this.eventListener.filterEventsBy(EyesClosedEvent.class), contains(instanceOf(EyesClosedEvent.class)));
    }

    @Test
    public void testOpenClose() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_opened, 0);
        this.detectorConsumesImage(R.drawable.eyes_closed, 1000);

        // Then
        assertThat(
                this.eventListener.filterEventsBy(EyesOpenedEvent.class, EyesClosedEvent.class),
                contains(
                        instanceOf(EyesOpenedEvent.class),
                        instanceOf(EyesClosedEvent.class)));
    }

    @Test
    public void testOpenCloseOpen() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_opened, 0);
        this.detectorConsumesImage(R.drawable.eyes_closed, 1000);
        this.detectorConsumesImage(R.drawable.eyes_opened, 2000);

        // Then
        assertThat(
                this.eventListener.filterEventsBy(EyesOpenedEvent.class, EyesClosedEvent.class),
                contains(
                        instanceOf(EyesOpenedEvent.class),
                        instanceOf(EyesClosedEvent.class),
                        instanceOf(EyesOpenedEvent.class)));
    }

    @Test
    public void shouldCreateSlowEyelidClosureEvent() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);
        this.detectorConsumesImage(R.drawable.eyes_opened, 501);

        // Then
        assertThat(this.eventListener.filterEventsBy(SlowEyelidClosureEvent.class), contains(instanceOf(SlowEyelidClosureEvent.class)));
    }

    @Test
    public void shouldCreateNormalEyeBlinkEvent() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);
        this.detectorConsumesImage(R.drawable.eyes_opened, 499);

        // Then
        assertThat(this.eventListener.filterEventsBy(NormalEyeBlinkEvent.class), contains(instanceOf(NormalEyeBlinkEvent.class)));
    }

    @Test
    public void shouldCreateSlowEyelidClosureEventAndNormalEyeBlinkEvent() {
        // When
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);
        this.detectorConsumesImage(R.drawable.eyes_opened, 501);
        this.detectorConsumesImage(R.drawable.eyes_closed, 600);
        this.detectorConsumesImage(R.drawable.eyes_opened, 600 + 499);

        // Then
        assertThat(
                this.eventListener.filterEventsBy(SlowEyelidClosureEvent.class, NormalEyeBlinkEvent.class),
                contains(instanceOf(SlowEyelidClosureEvent.class), instanceOf(NormalEyeBlinkEvent.class)));
    }

    @Test
    public void shouldCreateDrowsyEvent() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        clock.setNow(new Instant(5000));
        this.detectorConsumesImage(R.drawable.eyes_opened, 5000);

        clock.setNow(new Instant(5001));
        // dummy image in order to give DrowsyEventProducer a chance to produce a DrowsyEvent
        this.detectorConsumesImage(R.drawable.eyes_closed, 5001);

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(isA(DrowsyEvent.class)));
    }

    @Test
    public void shouldCreateDrowsyEventForEyesClosedTheWholeTime() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        clock.setNow(new Instant(15000));
        this.detectorConsumesImage(R.drawable.eyes_closed, 15000);

        // Then
        double perclos = 1.0; // > 0.15
        assertThat(this.eventListener.getEvents(), hasItem(new DrowsyEvent(new Instant(15000), perclos)));
    }

    @Test
    public void shouldCreateDrowsyEventForEyesClosedButNotYetOpenedWhenMaybeProducingDrowsyEvent() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        clock.setNow(new Instant(501));
        this.detectorConsumesImage(R.drawable.eyes_opened, 501);

        clock.setNow(new Instant(510));
        this.detectorConsumesImage(R.drawable.eyes_closed, 510);

        clock.setNow(new Instant(15000));
        this.detectorConsumesImage(R.drawable.eyes_closed, 15000);

        // Then
        double perclos = (501.0 + (15000.0 - 510.0)) / 15000.0; // = 0.9994 > 0.15
        assertThat(this.eventListener.getEvents(), hasItem(new DrowsyEvent(new Instant(15000), perclos)));
    }

    @Test
    public void shouldCreateNoDrowsyEvent() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        clock.setNow(new Instant(499));
        this.detectorConsumesImage(R.drawable.eyes_opened, 499);

        clock.setNow(new Instant(14000));
        this.detectorConsumesImage(R.drawable.eyes_closed_opened, 14000);

        // Then
        double perclos = 0.0;
        assertThat(this.eventListener.getEvents(), not(hasItem(isA(DrowsyEvent.class))));
    }

    @Test
    public void shouldCreateLikelyDrowsyEvent() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_closed, 0);

        clock.setNow(new Instant(1500));
        this.detectorConsumesImage(R.drawable.eyes_opened, 1500);

        clock.setNow(new Instant(1501));
        // dummy image in order to give DrowsyEventProducer a chance to produce a DrowsyEvent
        this.detectorConsumesImage(R.drawable.eyes_closed, 1501);

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(isA(LikelyDrowsyEvent.class)));
    }

    @Test
    public void shouldCreateAwakeEvent() {
        // Given
        MockedClock clock = new MockedClock();
        this.setup(clock);

        // When
        clock.setNow(new Instant(0));
        this.detectorConsumesImage(R.drawable.eyes_opened, 0);

        // Then
        assertThat(this.eventListener.getEvents(), hasItem(isA(AwakeEvent.class)));
    }

    private void detectorConsumesImage(final int imageResource, final int millis) {
        this.detector.receiveFrame(this.createFrame(imageResource, millis));
    }

    private Frame createFrame(final int imageResource, final int millis) {
        Bitmap bitmap = this.getBitmap(imageResource);
        return new Frame
                .Builder()
                .setBitmap(bitmap)
                .setTimestampMillis(millis)
                .build();
    }

    private Bitmap getBitmap(final int imageResource) {
        return BitmapFactory.decodeResource(this.appContext.getResources(), imageResource);
    }

    @NonNull
    private MultiProcessor.Factory<Face> createFactory() {
        return new MultiProcessor.Factory<Face>() {

            @Override
            public Tracker<Face> create(final Face face) {
                return EventTest.this.drowsyEventDetector.getGraphicFaceTracker();
            }
        };
    }

    private static class MockedClock implements Clock {

        private Instant now = new Instant(0);

        @Override
        public Instant now() {
            return this.now;
        }

        public void setNow(final Instant now) {
            this.now = now;
        }
    }
}
