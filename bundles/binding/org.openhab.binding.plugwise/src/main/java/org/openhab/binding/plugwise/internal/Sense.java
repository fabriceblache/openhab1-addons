/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plugwise.internal;

import java.util.Calendar;

import org.joda.time.DateTime;
import org.openhab.binding.plugwise.PlugwiseCommandType;
import org.openhab.binding.plugwise.protocol.AnnounceAwakeRequestMessage;
import org.openhab.binding.plugwise.protocol.AnnounceAwakeRequestMessage.AwakeReason;
import org.openhab.binding.plugwise.protocol.BroadcastGroupSwitchResponseMessage;
import org.openhab.binding.plugwise.protocol.InformationRequestMessage;
import org.openhab.binding.plugwise.protocol.InformationResponseMessage;
import org.openhab.binding.plugwise.protocol.Message;
import org.openhab.binding.plugwise.protocol.ModuleJoinedNetworkRequestMessage;
import org.openhab.binding.plugwise.protocol.SenseReportRequestMessage;

/**
 * A class that represents a Plugwise Sense device.
 *
 * The Sense is a wireless temperature/humidity sensor that switches on groups of devices depending on the current
 * temperature or humidity level. It also periodically reports back the current temperature and humidity levels.
 *
 * @author Wouter Born
 * @since 1.9.0
 */
public class Sense extends PlugwiseDevice {

    protected Stick stick;

    protected float humidity;
    protected float temperature;
    protected boolean triggeredState;

    // System variables as kept/maintained by the Sense hardware
    protected DateTime stamp;
    protected int recentLogAddress;
    protected String hardwareVersion;

    public Sense(String mac, Stick stick, String name) {
        super(mac, DeviceType.Sense, name);
        this.stick = stick;
    }

    public boolean getTriggeredState() {
        return triggeredState;
    }

    public boolean setTriggeredState(boolean state) {
        triggeredState = state;
        return true;
    }

    public boolean setTriggeredState(String state) {
        if (state != null) {
            if (state.equals("ON") || state.equals("TRIGGERED")) {
                return setTriggeredState(true);
            } else if (state.equals("OFF") || state.equals("NOT_TRIGGERED")) {
                return setTriggeredState(false);
            }
        }
        return true;
    }

    public void updateInformation() {
        InformationRequestMessage message = new InformationRequestMessage(MAC);
        stick.sendMessage(message);
    }

    @Override
    public boolean processMessage(Message message) {
        if (message != null) {

            Calendar timestamp;

            switch (message.getType()) {
                case ANNOUNCE_AWAKE_REQUEST:
                    AwakeReason awakeReason = ((AnnounceAwakeRequestMessage) message).getAwakeReason();
                    if (awakeReason == AwakeReason.Maintenance || awakeReason == AwakeReason.WakeupButton) {
                        updateInformation();
                    }
                    timestamp = ((AnnounceAwakeRequestMessage) message).getDateTimeReceived();
                    postUpdate(MAC, PlugwiseCommandType.LASTSEEN, timestamp);
                    return true;

                case BROADCAST_GROUP_SWITCH_RESPONSE:
                    triggeredState = ((BroadcastGroupSwitchResponseMessage) message).getPowerState();
                    timestamp = ((BroadcastGroupSwitchResponseMessage) message).getDateTimeReceived();
                    postUpdate(MAC, PlugwiseCommandType.TRIGGERED, triggeredState);
                    postUpdate(MAC, PlugwiseCommandType.TRIGGEREDSTAMP, timestamp);
                    postUpdate(MAC, PlugwiseCommandType.LASTSEEN, timestamp);
                    return true;

                case DEVICE_INFORMATION_RESPONSE:
                    stamp = new DateTime(((InformationResponseMessage) message).getYear(),
                            ((InformationResponseMessage) message).getMonth(), 1, 0, 0)
                                    .plusMinutes(((InformationResponseMessage) message).getMinutes());
                    recentLogAddress = ((InformationResponseMessage) message).getLogAddress();
                    hardwareVersion = ((InformationResponseMessage) message).getHardwareVersion();
                    timestamp = ((InformationResponseMessage) message).getDateTimeReceived();
                    postUpdate(MAC, PlugwiseCommandType.LASTSEEN, timestamp);
                    return true;

                case MODULE_JOINED_NETWORK_REQUEST:
                    timestamp = ((ModuleJoinedNetworkRequestMessage) message).getDateTimeReceived();
                    postUpdate(MAC, PlugwiseCommandType.LASTSEEN, timestamp);
                    return true;

                case SENSE_REPORT_REQUEST:
                    humidity = ((SenseReportRequestMessage) message).getHumidity();
                    temperature = ((SenseReportRequestMessage) message).getTemperature();
                    timestamp = ((SenseReportRequestMessage) message).getDateTimeReceived();
                    postUpdate(MAC, PlugwiseCommandType.HUMIDITY, humidity);
                    postUpdate(MAC, PlugwiseCommandType.TEMPERATURE, temperature);
                    postUpdate(MAC, PlugwiseCommandType.HUMIDITYSTAMP, timestamp);
                    postUpdate(MAC, PlugwiseCommandType.TEMPERATURESTAMP, timestamp);
                    postUpdate(MAC, PlugwiseCommandType.LASTSEEN, timestamp);
                    return true;

                default:
                    // Let's have the Stick a go at this message
                    return stick.processMessage(message);
            }

        } else {
            return false;
        }
    }

    @Override
    public boolean postUpdate(String MAC, PlugwiseCommandType type, Object value) {
        if (MAC != null && type != null && value != null) {
            stick.postUpdate(MAC, type, value);
            return true;
        } else {
            return false;
        }

    }
}
