package org.mtgpeasant.perfectdeck.common.utils;

import java.util.Collection;
import java.util.Objects;

public class UnorderedCollections {
    public static int hashCode(Collection<?> collection) {
        if (collection == null) {
            return 0;
        } else {
            int hash = 1;
            int size = collection.size();
            for (Object o : collection) {
                hash += Objects.hashCode(o);
            }
            return hash;
        }
    }

    public static boolean equals(Collection<?> collection1, Collection<?> collection2) {
        if (collection1 == collection2) {
            return true;
        }
        if (collection1 == null || collection2 == null) {
            return false;
        }
        if (collection1.size() != collection2.size()) {
            return false;
        }
        // TODO: wrong (same cards but different numbers)
        for (Object o : collection1) {
            if (!collection2.contains(o)) {
                return false;
            }
        }
        return true;
    }
}
