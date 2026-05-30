package com.yzddmr6.prismspace.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HackTest {

	@Test public void memberFieldReadsAndWritesPrivateField() {
		final Hack.HackedField<Target, Integer> field = Hack.into(Target.class).field("value").fallbackTo(0);
		final Target target = new Target(3);

		assertFalse(field.isAbsent());
		assertEquals(Integer.valueOf(3), field.get(target));

		field.set(target, 7);
		assertEquals(7, target.value);
	}

	@Test public void fallbackFieldIsAbsentAndReturnsFallbackValue() {
		final Hack.HackedField<Target, Integer> field = Hack.into(Target.class).field("missing").fallbackTo(42);

		assertTrue(field.isAbsent());
		assertEquals(Integer.valueOf(42), field.get(new Target(1)));
	}

	private static final class Target {
		private int value;

		private Target(final int value) {
			this.value = value;
		}
	}
}
