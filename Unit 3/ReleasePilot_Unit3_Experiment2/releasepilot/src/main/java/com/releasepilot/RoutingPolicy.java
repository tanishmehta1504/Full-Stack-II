package com.releasepilot;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
/** Stable synthetic-visitor cohorts, rather than changing backend on every request. */
public final class RoutingPolicy {
 private RoutingPolicy(){}
 public static String select(String key,int percent){if(percent<0||percent>100)throw new IllegalArgumentException("Invalid percentage");var crc=new CRC32();crc.update(key.getBytes(StandardCharsets.UTF_8));return crc.getValue()%100<percent?"canary":"stable";}
}
