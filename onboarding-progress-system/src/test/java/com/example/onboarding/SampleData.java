package com.example.onboarding;

import java.util.ArrayList;
import java.util.List;

/**
 * The sample data that Flyway puts into every test database (db/migration V2, V4, V5, V9).
 * Tests use these values instead of writing the numbers again, so that a change to the sample data
 * only needs to be reflected here.
 *
 * <p>The values are written by hand from the migrations on purpose: they must not be calculated by the
 * code under test, otherwise a bug in that code would also change the expected values.
 */
public final class SampleData {

    private SampleData() {
    }

    // --- tasks (V2: 1-5, task 5 inactive; V9: 6, the sample task with a link) ---

    /** Task ids in display order (sort_order 10, 20, ... 60). */
    public static final List<Long> TASK_IDS_IN_ORDER = List.of(1L, 2L, 3L, 4L, 5L, 6L);

    public static final int TASK_COUNT = TASK_IDS_IN_ORDER.size();

    public static final long FIRST_TASK = 1L;
    public static final long LAST_TASK = 6L;

    /** sort_order given to the next new task: the last sample task has 60, the step is 10. */
    public static final int NEXT_SORT_ORDER = 70;

    // --- users and assignments (V4) ---

    public static final long YAMADA = 1L;
    public static final long SATO = 2L;
    public static final long SUZUKI = 3L;

    /** Tasks assigned to 鈴木, by task id. */
    public static final List<Long> TASKS_OF_SUZUKI = List.of(1L, 2L);

    /** Active tasks not assigned to 鈴木 yet, in display order. */
    public static final List<Long> TASKS_ASSIGNABLE_TO_SUZUKI = List.of(3L, 4L, 6L);

    /** Active tasks not assigned to 山田 yet (he has done tasks 1-4). */
    public static final List<Long> TASKS_ASSIGNABLE_TO_YAMADA = List.of(6L);

    /** A copy of the list with the two values swapped, e.g. the display order after moving a task. */
    public static List<Long> swapped(List<Long> ids, long a, long b) {
        List<Long> copy = new ArrayList<>(ids);
        int indexOfA = copy.indexOf(a);
        int indexOfB = copy.indexOf(b);
        copy.set(indexOfA, b);
        copy.set(indexOfB, a);
        return copy;
    }
}
