package org.mtgpeasant.perfectdeck.goldfish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Permanents extends ArrayList<Permanent> implements Cloneable {
    /**
     * Finds first permanent matching the given filter
     *
     * @param filter permanent filter
     * @return matching permanent
     */
    public Optional<Permanent> findFirst(Predicate<Permanent> filter) {
        return stream().filter(filter).findFirst();
    }

    /**
     * Finds all permanents matching the given filter
     *
     * @param filter permanent filter
     * @return matching permanents
     */
    public List<Permanent> find(Predicate<Permanent> filter) {
        return stream().filter(filter).collect(Collectors.toList());
    }

    /**
     * Counts all permanents matching the given filter
     *
     * @param filter permanent filter
     * @return matching permanents count
     */
    public int count(Predicate<Permanent> filter) {
        return (int) stream().filter(filter).count();
    }

    /**
     * Deep cloning
     */
    @Override
    public Permanents clone() {
        Permanents permanents = (Permanents) super.clone();
        for (int i = 0; i < size(); i++) {
            permanents.set(i, permanents.get(i).clone());
        }
        return permanents;
    }
}
