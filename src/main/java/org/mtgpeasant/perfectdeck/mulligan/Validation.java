package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class Validation {
    final List<String> errors = new ArrayList<>();

    public void error(String message) {
        errors.add(message);
    }
}
