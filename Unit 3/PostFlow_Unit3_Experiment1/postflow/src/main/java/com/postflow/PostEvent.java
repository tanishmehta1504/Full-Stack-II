package com.postflow;
/** Event ID is the business idempotency key; generation identifies a deliberate DLQ replay. */
public record PostEvent(String eventId,int generation,String title,String content,String channel,long scheduledAt) {}
