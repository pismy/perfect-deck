package org.mtgpeasant.stats.matchers;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class Validation {
//    final List<String> warnings = new ArrayList<>();
    final List<String> errors = new ArrayList<>();

//    public void warning(String message) {
//        warnings.add(message);
//    }

    public void error(String message) {
        errors.add(message);
    }
}
