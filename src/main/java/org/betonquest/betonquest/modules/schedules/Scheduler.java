package org.betonquest.betonquest.modules.schedules;

import lombok.CustomLog;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.config.QuestPackage;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.EventID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@CustomLog
public class Scheduler {
    private final List<EventTimer> timers;

    public Scheduler() {
        timers = new ArrayList<>();
        for (final QuestPackage pack : Config.getPackages().values()) {
            final ConfigurationSection schedules = pack.getConfig().getConfigurationSection("schedules");
            if (schedules == null) {
                continue;
            }
            for (final String scheduleKey : schedules.getKeys(false)) {
                final ConfigurationSection schedule = schedules.getConfigurationSection(scheduleKey);
                if (schedule == null) {
                    continue;
                }
                try {
                    parseSchedule(pack, schedule);
                } catch (final InstructionParseException e) {
                    LOG.warn(pack, "Could not load schedule '" + scheduleKey + "' from QuestPackage '" + pack.getPackagePath() + "': " + e.getMessage(), e);
                }
            }
        }
    }

    private void parseSchedule(final QuestPackage pack, final ConfigurationSection schedule) throws InstructionParseException {
        final String time = schedule.getString("time");
        if (time == null) {
            throw new InstructionParseException("Schedule time is not defined");
        }
        if (!schedule.isString("time")) {
            throw new InstructionParseException("Schedule time is no string");
        }

        final long timeStamp = getTimestamp(key);
        if (timeStamp < 0) {
            return;
        }
        try {
            final String[] events = value.split(",");
            final EventID[] eventIDS = new EventID[events.length];
            for (int i = 0; i < events.length; i++) {
                eventIDS[i] = new EventID(pack, events[i]);
            }
            timers.add(new EventTimer(timeStamp, eventIDS));
        } catch (final ObjectNotFoundException e) {
            LOG.warn(pack, "Could not load static event '" + pack.getPackagePath() + "." + key + "': " + e.getMessage(), e);
        }
    }

    private long getTimestamp(final String hour) {

        final Date time = new Date();
        final String timeString = new SimpleDateFormat("dd.MM.yy", Locale.ROOT).format(time) + " " + hour;

        long timeStamp = -1;
        try {
            timeStamp = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.ROOT).parse(timeString).getTime();
        } catch (final ParseException e) {
            LOG.warn("Error in time setting in static event declaration: " + hour, e);
        }

        if (timeStamp < new Date().getTime()) {
            timeStamp += 24 * 60 * 60 * 1000;
        }
        return timeStamp;
    }


    public void cancel() {
        for (final EventTimer timer : timers) {
            timer.cancel();
        }
    }

    private static class EventTimer extends TimerTask {

        protected final EventID[] event;

        public EventTimer(final long timeStamp, final EventID... eventID) {
            super();
            event = eventID == null ? null : Arrays.copyOf(eventID, eventID.length);
            new Timer().schedule(this, timeStamp - new Date().getTime(), 24 * 60 * 60 * 1000);
        }

        @Override
        public void run() {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final EventID eventID : event) {
                        BetonQuest.event(null, eventID);
                    }
                }
            }.runTask(BetonQuest.getInstance());
        }
    }

}
