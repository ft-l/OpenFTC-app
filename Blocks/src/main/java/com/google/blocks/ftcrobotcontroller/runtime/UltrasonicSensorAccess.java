// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.util.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtUltrasonicSensor;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;
import junit.framework.Assert;

/**
 * A class that provides JavaScript access to an {@link UltrasonicSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class UltrasonicSensorAccess extends HardwareAccess<UltrasonicSensor> {
  private final UltrasonicSensor ultrasonicSensor;

  UltrasonicSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem,
      HardwareMap hardwareMap, Class<? extends HardwareDevice> deviceType) {
    super(blocksOpMode, hardwareItem, hardwareMap, UltrasonicSensor.class);
    Assert.assertTrue(deviceType == UltrasonicSensor.class);
    this.ultrasonicSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtUltrasonicSensor.class}, methodName = "getUltrasonicLevel")
  public double getUltrasonicLevel() {
    checkIfStopRequested();
    if (ultrasonicSensor != null) {
      return ultrasonicSensor.getUltrasonicLevel();
    }
    return 0.0;
  }
}
