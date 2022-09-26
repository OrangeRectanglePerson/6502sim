package Extras;

import Devices.Device;

import java.util.Comparator;

public class DeviceComparator implements Comparator<Device> {
    @Override
    public int compare(Device o1, Device o2) {
        return Integer.compare(Short.toUnsignedInt(o1.getStartAddress()), Short.toUnsignedInt(o2.getStartAddress()));
    }
}
