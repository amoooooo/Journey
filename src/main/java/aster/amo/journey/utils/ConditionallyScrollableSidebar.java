package aster.amo.journey.utils;


import eu.pb4.sidebars.api.Sidebar;
import eu.pb4.sidebars.api.lines.SidebarLine;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ConditionallyScrollableSidebar extends Sidebar {
    protected Object2LongMap<ServerGamePacketListenerImpl> position = new Object2LongArrayMap();
    protected int scrollTickNumber;
    protected Predicate<ServerGamePacketListenerImpl> condition = (player) -> {
        return true;
    };

    public ConditionallyScrollableSidebar(Sidebar.Priority priority, int scrollTickNumber) {
        super(priority);
        this.scrollTickNumber = scrollTickNumber;
    }

    public ConditionallyScrollableSidebar(Component title, Sidebar.Priority priority, int scrollTickNumber, Predicate<ServerGamePacketListenerImpl> condition) {
        super(title, priority);
        this.scrollTickNumber = scrollTickNumber;
        this.condition = condition;
    }

    public int getTicksPerLine() {
        return this.scrollTickNumber;
    }

    public void setTicksPerLine(int scrollTickNumber) {
        this.scrollTickNumber = scrollTickNumber;
    }

    public List<SidebarLine> getLinesFor(ServerGamePacketListenerImpl handler) {
        this.sortIfDirty();
        if (!this.condition.test(handler)) {
            return elements;
        } else {
            long pos = this.position.getLong(handler);
            ++pos;
            int index = (int)pos / this.scrollTickNumber;
            if (index + 14 > this.elements.size()) {
                pos = 0L;
                index = 0;
            }

            this.position.put(handler, pos);
            return this.elements.subList(index, Math.min(index + 14, this.elements.size()));
        }
    }

    public void removePlayer(ServerGamePacketListenerImpl handler) {
        super.removePlayer(handler);
        this.position.removeLong(handler);
    }
}
