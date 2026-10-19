package com.example.onboarding.service;

import java.util.List;

/**
 * The sub-items of one assignment and whether each is checked, for showing on a page.
 */
public record Checklist(List<Item> items) {

    public static final Checklist EMPTY = new Checklist(List.of());

    public record Item(Long id, String label, boolean checked) {
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int total() {
        return items.size();
    }

    public long checked() {
        return items.stream().filter(Item::checked).count();
    }
}
