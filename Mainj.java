import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ExtractFirstTopLevelStream {
    public static void main(String[] args) {
        String text = """
            Voici [un exemple
            [avec des crochets]
            imbriqués] sur
            plusieurs lignes [et un autre].
            """;

        extractFirstTopLevel(text).ifPresent(System.out::println);
    }

    public static Optional<String> extractFirstTopLevel(String input) {
        AtomicInteger depth = new AtomicInteger(0);
        AtomicInteger start = new AtomicInteger(-1);
        StringBuilder result = new StringBuilder();

        IntStream.range(0, input.length()).takeWhile(i -> result.isEmpty())
                .forEach(i -> {
                    char c = input.charAt(i);

                    if (c == '[') {
                        if (depth.get() == 0) start.set(i + 1);
                        depth.incrementAndGet();
                    } else if (c == ']') {
                        if (depth.get() > 0) {
                            depth.decrementAndGet();
                            if (depth.get() == 0 && start.get() >= 0) {
                                result.append(input, start.get(), i);
                            }
                        }
                    }
                });

        return result.isEmpty() ? Optional.empty() : Optional.of(result.toString());
    }
}