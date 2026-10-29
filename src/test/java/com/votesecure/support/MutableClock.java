package com.votesecure.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock that tests can move forward or backward without waiting for real time. */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(LocalDateTime initial) {
        this(initial, ZoneId.systemDefault());
    }

    private MutableClock(LocalDateTime initial, ZoneId zone) {
        this.zone = zone;
        setTo(initial);
    }

    public void setTo(LocalDateTime moment) {
        this.instant = moment.atZone(zone).toInstant();
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(LocalDateTime.ofInstant(instant, newZone), newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
