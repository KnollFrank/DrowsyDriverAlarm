package com.google.android.gms.samples.vision.face.facetracker.listener;

import com.google.android.gms.samples.vision.face.facetracker.event.DurationEvent;
import com.google.android.gms.samples.vision.face.facetracker.event.EyesClosedEvent;
import com.google.android.gms.samples.vision.face.facetracker.event.EyesOpenedEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

abstract class DurationEventProducer extends EventProducer {

    private EyesClosedEvent eyesClosedEvent;

    DurationEventProducer(final EventBus eventBus) {
        super(eventBus);
    }

    @Subscribe
    public void recordEyesClosedEvent(final EyesClosedEvent eyesClosedEvent) {
        this.eyesClosedEvent = eyesClosedEvent;
    }

    @Subscribe
    public void recordEyesOpenedEventAndPostDurationEvent(final EyesOpenedEvent eyesOpenedEvent) {
        if(this.eyesClosedEvent == null) {
            return;
        }

        final long duration = eyesOpenedEvent.getTimestampMillis() - this.eyesClosedEvent.getTimestampMillis();
        if(this.shallCreateEventFor(duration)) {
            this.postEvent(this.createDurationEvent(this.eyesClosedEvent.getTimestampMillis(), duration));
        }
    }

    protected abstract boolean shallCreateEventFor(final long duration);

    protected abstract DurationEvent createDurationEvent(final long timestampMillis, final long duration);
}
