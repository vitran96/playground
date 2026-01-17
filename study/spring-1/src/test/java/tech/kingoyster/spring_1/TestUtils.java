package tech.kingoyster.spring_1;

import java.util.Random;
import java.util.stream.Stream;

public class TestUtils {
    public static Stream<Integer> randomNumbers() {
        Random random = new Random();
        return Stream.generate(() -> random.nextInt(100)) // Generate random integers up to 99
                .limit(10); // Generate 10 random numbers
    }
}
