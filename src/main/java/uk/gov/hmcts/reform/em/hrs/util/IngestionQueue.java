package uk.gov.hmcts.reform.em.hrs.util;

import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;

import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nonnull;

public enum IngestionQueue {
    INSTANCE;

    private LinkedBlockingQueue<HearingRecordingDto> queue;

    public static Builder builder() {
        return new Builder();
    }

    private void init(@Nonnull final Integer queueCapacity) {
        queue = new LinkedBlockingQueue<>(queueCapacity);
    }

    public boolean offer(final HearingRecordingDto dto) {
        return queue.offer(dto);
    }

    public HearingRecordingDto poll() {
        return queue.poll();
    }

    public void clear() {
        queue.clear();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    public static class Builder {
        private static final int DEFAULT_CAPACITY = 1000;
        private int capacity;

        private Builder() {
        }

        public Builder capacity(final int capacity) {
            this.capacity = capacity;
            return this;
        }

        public IngestionQueue build() {
            final IngestionQueue instance = IngestionQueue.INSTANCE;
            if (capacity == 0) {
                instance.init(DEFAULT_CAPACITY);
            } else {
                instance.init(capacity);
            }

            return instance;
        }
    }

}
