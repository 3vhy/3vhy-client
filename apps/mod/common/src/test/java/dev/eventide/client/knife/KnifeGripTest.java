package dev.eventide.client.knife;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Every fist fits round its knife's grip: no knife pokes out through the fingers. */
class KnifeGripTest {
	@Test
	@Disabled("Work in progress: the measured band still takes in the guard above the index finger")
	void fistsFitTheirGrips() {
		List<String> report = new ArrayList<>();
		List<String> bad = new ArrayList<>();
		for (KnifeModel model : KnifeModel.values()) {
			float[] band = KnifeHand.fingerBand(model);
			float[] reach = model.reach(band[0], band[1]);
			if (reach == null) continue;
			float needA = Math.max(-reach[0], reach[1]);
			float needB = Math.max(-reach[2], reach[3]);
			String line = String.format(Locale.ROOT, "%-15s grip %5.2f..%5.2f  handle %.2f x %.2f  fist %.2f x %.2f", model.label, band[0], band[1],
					needA, needB, model.handleA, model.handleB);
			report.add(line);
			if (needA > model.handleA + 0.08f || needB > model.handleB + 0.08f) bad.add(line);
		}
		report.forEach(System.out::println);
		assertTrue(bad.isEmpty(), "fists that don't fit their grip:\n" + String.join("\n", bad));
	}
}
