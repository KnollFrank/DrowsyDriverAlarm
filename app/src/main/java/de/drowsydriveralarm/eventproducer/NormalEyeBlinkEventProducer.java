package de.drowsydriveralarm.eventproducer;

import com.google.common.eventbus.EventBus;

import org.joda.time.Duration;
import org.joda.time.Interval;

import de.drowsydriveralarm.event.DurationEvent;
import de.drowsydriveralarm.event.NormalEyeBlinkEvent;

public class NormalEyeBlinkEventProducer extends DurationEventProducer {

    private final Duration slowEyelidClosureMinDuration;

    public NormalEyeBlinkEventProducer(final Duration slowEyelidClosureMinDuration, final EventBus eventBus) {
        super(eventBus);
        this.slowEyelidClosureMinDuration = slowEyelidClosureMinDuration;
    }

    @Override
    protected boolean shallCreateEventFor(final Duration duration) {
        return this.isNormalEyeBlink(duration);
    }

    private boolean isNormalEyeBlink(final Duration duration) {
        return duration.isShorterThan(this.slowEyelidClosureMinDuration);
    }

    @Override
    protected DurationEvent createDurationEvent(final Interval interval) {
        return new NormalEyeBlinkEvent(interval.getStart().toInstant(), interval.toDuration());
    }
}
