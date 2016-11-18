package com.google.android.gms.samples.vision.face.facetracker.listener;

import com.google.android.gms.samples.vision.face.facetracker.PERCLOSCalculator;
import com.google.android.gms.samples.vision.face.facetracker.event.DrowsyEvent;
import com.google.android.gms.samples.vision.face.facetracker.event.SlowEyelidClosureEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class DrowsyEventProducer extends EventProducer {

    private final long timeWindowMillis;
    private final List<SlowEyelidClosureEvent> slowEyelidClosureEvents = new ArrayList<SlowEyelidClosureEvent>();

    public DrowsyEventProducer(final EventBus eventBus, final long timeWindowMillis) {
        super(eventBus);
        this.timeWindowMillis = timeWindowMillis;
    }

    @Subscribe
    public void recordEyesClosedEvent(final SlowEyelidClosureEvent slowEyelidClosureEvent) {
        this.slowEyelidClosureEvents.add(slowEyelidClosureEvent);
    }

    public void evaluate(final long timestampMillis) {
        double perclos = new PERCLOSCalculator().calculatePERCLOS(this.slowEyelidClosureEvents, this.timeWindowMillis);
        if(perclos >= 0.15) {
            this.postEvent(new DrowsyEvent(timestampMillis, perclos));
        }
    }
}
