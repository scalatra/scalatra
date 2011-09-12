package org.scalatra.test;

import java.util.*;
import org.apache.commons.lang3.reflect.MethodUtils;

public enum DispatcherType {
    REQUEST(1),
    FORWARD(2),
    INCLUDE(4),
    ERROR(8),
    ASYNC(16);

    private int intValue;

    private DispatcherType(int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }

    public static int intValue(Set<DispatcherType> dispatcherTypes) {
        int value = 0;
        for (DispatcherType dispatcherType : dispatcherTypes) {
            value |= dispatcherType.intValue();
        }
        return value;
    }

    @SuppressWarnings(value = "unchecked") // yeah... I know
    public static <E extends Enum<E>> EnumSet<E> convert(
            Set<DispatcherType> dispatcherTypes, String className)
        throws Exception
    {
        Set<E> result = new HashSet<E>();
        Class<?> enumClass = Class.forName(className);
        for (DispatcherType dispatcherType : dispatcherTypes) {
            result.add((E) MethodUtils.invokeStaticMethod(Enum.class, "valueOf",
                enumClass, dispatcherType.name()));
        }
        return EnumSet.copyOf(result);
    }
}
